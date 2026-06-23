package com.nickax.nexus.core.data;

import com.nickax.nexus.api.data.Backend;
import com.nickax.nexus.api.data.Cache;
import com.nickax.nexus.api.data.DataStore;
import com.nickax.nexus.api.data.KeyMapper;
import com.nickax.nexus.api.data.WritePolicy;
import com.nickax.nexus.api.lock.LockService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Default {@link DataStore}: a {@link Cache} over a {@link Backend}, keyed via a
 * {@link KeyMapper} and guarded by a {@link LockService} for read-modify-write.
 *
 * <p>Supports two write policies:
 * <ul>
 *   <li><b>Write-through</b> (default, 4-arg constructor): every {@code save} persists
 *       to the backend immediately; {@code flush} pushes the whole cache.</li>
 *   <li><b>Write-behind</b> (6-arg constructor): {@code save} updates the cache and
 *       marks the key dirty; {@code delete} records a tombstone; a periodic flush
 *       persists dirty entries and applies tombstones on the supplied scheduler.</li>
 * </ul>
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public final class DataStoreImpl<K, V> implements DataStore<K, V> {

    private static final System.Logger LOGGER = System.getLogger("Nexus");

    private final KeyMapper<K> keyMapper;
    private final Cache<V> cache;
    private final Backend<V> backend;
    private final LockService lockService;
    private final Set<String> dirty = ConcurrentHashMap.newKeySet();
    private final Set<String> tombstones = ConcurrentHashMap.newKeySet();
    private final boolean writeBehind;

    /**
     * Creates a write-through data store (no background scheduler).
     *
     * @param keyMapper   maps typed keys to string keys
     * @param cache       the hot tier
     * @param backend     the durable tier
     * @param lockService guards read-modify-write per key
     */
    public DataStoreImpl(@NotNull KeyMapper<K> keyMapper, @NotNull Cache<V> cache,
                         @NotNull Backend<V> backend, @NotNull LockService lockService) {
        this(keyMapper, cache, backend, lockService, WritePolicy.writeThrough(), null);
    }

    /**
     * Creates a data store with an explicit write policy. When the policy is
     * write-behind and a scheduler is supplied, a periodic flush is scheduled.
     *
     * @param keyMapper   the key mapper
     * @param cache       the hot tier
     * @param backend     the durable tier
     * @param lockService the per-key lock service
     * @param writePolicy the write policy
     * @param scheduler   the scheduler for periodic write-behind flushes, or {@code null}
     */
    public DataStoreImpl(@NotNull KeyMapper<K> keyMapper, @NotNull Cache<V> cache,
                         @NotNull Backend<V> backend, @NotNull LockService lockService,
                         @NotNull WritePolicy writePolicy, @Nullable ScheduledExecutorService scheduler) {
        this.keyMapper = keyMapper;
        this.cache = cache;
        this.backend = backend;
        this.lockService = lockService;
        this.writeBehind = writePolicy instanceof WritePolicy.WriteBehind;
        if (writePolicy instanceof WritePolicy.WriteBehind(Duration interval) && scheduler != null) {
            long ms = Math.max(1L, interval.toMillis());
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    flush().join();
                } catch (RuntimeException e) {
                    LOGGER.log(System.Logger.Level.WARNING, "Periodic flush failed; retrying next interval", e);
                }
            }, ms, ms, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Looks up a value from the cache, falling back to the backend on a miss.
     * A backend hit populates the cache before returning.
     *
     * @param key the key to look up
     * @return an optional containing the value, or empty if not found in either tier
     */
    @Override
    public @NotNull CompletableFuture<Optional<V>> find(@NotNull K key) {
        String id = keyMapper.toKey(key);
        return cache.get(id).thenCompose(cached -> {
            if (cached.isPresent()) {
                return CompletableFuture.completedFuture(cached);
            }
            return backend.get(id).thenCompose(stored -> {
                if (stored.isEmpty()) {
                    return CompletableFuture.completedFuture(Optional.empty());
                }
                return cache.put(id, stored.get()).thenApply(ignored -> stored);
            });
        });
    }

    /**
     * Returns the existing value for {@code key}, or creates and saves a new one via
     * {@code factory} if absent. The create+save is performed inside a lock to
     * prevent duplicate creation under concurrent callers.
     *
     * @param key     the key to look up or create
     * @param factory supplies the new value when the key is absent
     * @return the existing or newly created value
     */
    @Override
    public @NotNull CompletableFuture<V> getOrCreate(@NotNull K key, @NotNull Supplier<V> factory) {
        return lockService.withLock(keyMapper.toKey(key), () -> find(key).thenCompose(found -> {
            if (found.isPresent()) {
                return CompletableFuture.completedFuture(found.get());
            }
            V created = factory.get();
            return save(key, created).thenApply(ignored -> created);
        }));
    }

    /**
     * Persists a value. Under write-through the backend is updated immediately;
     * under write-behind only the cache is updated and the key is marked dirty for
     * the next flush. Any pending tombstone for the key is cleared.
     *
     * @param key   the key to save under
     * @param value the value to persist
     * @return a future that completes when the operation is done
     */
    @Override
    public @NotNull CompletableFuture<Void> save(@NotNull K key, @NotNull V value) {
        String id = keyMapper.toKey(key);
        if (writeBehind) {
            return cache.put(id, value).thenRun(() -> {
                tombstones.remove(id);
                dirty.add(id);
            });
        }
        return cache.put(id, value).thenCompose(ignored -> backend.put(id, value));
    }

    /**
     * Applies {@code mutator} to the current value and saves the result, inside a
     * lock to prevent lost-update races.
     *
     * @param key     the key to update
     * @param mutator the function to apply to the current value
     * @return the updated value
     * @throws java.util.NoSuchElementException if the key does not exist
     */
    @Override
    public @NotNull CompletableFuture<V> update(@NotNull K key, @NotNull UnaryOperator<V> mutator) {
        return lockService.withLock(keyMapper.toKey(key), () -> find(key).thenCompose(found -> {
            if (found.isEmpty()) {
                return CompletableFuture.failedFuture(
                        new NoSuchElementException("No value for key " + key));
            }
            V updated = mutator.apply(found.get());
            return save(key, updated).thenApply(ignored -> updated);
        }));
    }

    /**
     * Removes the value from both cache and backend. Under write-behind the backend
     * removal is deferred: the key is tombstoned so the next flush issues the delete
     * and skips any pending dirty write for the same key.
     *
     * @param key the key to remove
     * @return a future that completes when the cache entry is gone (backend removal
     *         may be deferred under write-behind)
     */
    @Override
    public @NotNull CompletableFuture<Void> delete(@NotNull K key) {
        String id = keyMapper.toKey(key);
        if (writeBehind) {
            return cache.remove(id).thenRun(() -> {
                dirty.remove(id);
                tombstones.add(id);
            });
        }
        return cache.remove(id).thenCompose(ignored -> backend.remove(id));
    }

    /**
     * Returns whether a value exists for the given key, checking the cache first and
     * falling back to the backend.
     *
     * @param key the key to test
     * @return {@code true} if a value exists in either tier
     */
    @Override
    public @NotNull CompletableFuture<Boolean> contains(@NotNull K key) {
        String id = keyMapper.toKey(key);
        return cache.contains(id).thenCompose(inCache ->
                inCache ? CompletableFuture.completedFuture(true) : backend.contains(id));
    }

    /**
     * Forces a read from the backend, bypassing the cache, and populates the cache
     * with the result. Useful for reloading stale in-memory state.
     *
     * @param key the key to reload
     * @return the value loaded from the backend, or empty if absent
     */
    @Override
    public @NotNull CompletableFuture<Optional<V>> load(@NotNull K key) {
        String id = keyMapper.toKey(key);
        return backend.get(id).thenCompose(stored -> {
            if (stored.isEmpty()) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            return cache.put(id, stored.get()).thenApply(ignored -> stored);
        });
    }

    /**
     * Flushes in-memory state to the backend.
     *
     * <p>Under <b>write-through</b>: pushes the entire cache to the backend (useful
     * for callers that pre-populate the cache).
     *
     * <p>Under <b>write-behind</b>: snapshots the dirty and tombstone sets atomically,
     * then persists dirty entries and applies tombstones. If a key is both dirty and
     * tombstoned the tombstone wins.
     *
     * @return a future that completes when all backend operations are done
     */
    @Override
    public @NotNull CompletableFuture<Void> flush() {
        if (!writeBehind) {
            // write-through: push the whole cache (used by callers that pre-populate the cache)
            return cache.all().thenCompose(entries -> {
                CompletableFuture<?>[] writes = entries.entrySet().stream()
                        .map(e -> backend.put(e.getKey(), e.getValue()))
                        .toArray(CompletableFuture[]::new);
                return CompletableFuture.allOf(writes);
            });
        }
        // write-behind: persist dirty entries, apply tombstones, then clear both sets.
        // If a key is both dirty and tombstoned (a concurrent save+delete), the tombstone wins.
        Set<String> dirtySnapshot = Set.copyOf(dirty);
        Set<String> tombstoneSnapshot = Set.copyOf(tombstones);
        dirty.removeAll(dirtySnapshot);
        tombstones.removeAll(tombstoneSnapshot);
        CompletableFuture<?>[] ops = java.util.stream.Stream.concat(
                dirtySnapshot.stream()
                        .filter(id -> !tombstoneSnapshot.contains(id))
                        .map(id -> cache.get(id).thenCompose(value ->
                                value.map(v -> backend.put(id, v)).orElseGet(() -> CompletableFuture.completedFuture(null)))),
                tombstoneSnapshot.stream().map(backend::remove)
        ).toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(ops);
    }

    /**
     * Returns all entries, merging the backend with the cache (cache wins on
     * collision) and converting string keys back to typed keys via the key mapper.
     *
     * @return a future resolving to all known entries
     */
    @Override
    public @NotNull CompletableFuture<Map<K, V>> all() {
        return backend.all().thenCompose(backendAll -> cache.all().thenApply(cacheAll -> {
            Map<String, V> merged = new HashMap<>(backendAll);
            merged.putAll(cacheAll); // cache wins
            Map<K, V> result = new HashMap<>();
            merged.forEach((id, value) -> result.put(keyMapper.fromKey(id), value));
            return result;
        }));
    }
}
