package com.nickax.nexus.core.data;

import com.nickax.nexus.api.data.Cache;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link Cache} backed by a {@link ConcurrentHashMap}. Operations
 * complete immediately; the returned futures are already done.
 *
 * @param <V> the value type
 */
public final class MemoryCache<V> implements Cache<V> {

    private final Map<String, V> map = new ConcurrentHashMap<>();

    /**
     * Returns the cached value for {@code key}, or empty if absent.
     *
     * @param key the cache key
     * @return an already-completed future containing the optional value
     */
    @Override
    public @NotNull CompletableFuture<Optional<V>> get(@NotNull String key) {
        return CompletableFuture.completedFuture(Optional.ofNullable(map.get(key)));
    }

    /**
     * Stores the value under {@code key}.
     *
     * @param key   the cache key
     * @param value the value to cache
     * @return an already-completed future
     */
    @Override
    public @NotNull CompletableFuture<Void> put(@NotNull String key, @NotNull V value) {
        map.put(key, value);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Evicts the entry for {@code key}, if present.
     *
     * @param key the cache key to evict
     * @return an already-completed future
     */
    @Override
    public @NotNull CompletableFuture<Void> remove(@NotNull String key) {
        map.remove(key);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns whether {@code key} is present in the cache.
     *
     * @param key the cache key to test
     * @return an already-completed future resolving to {@code true} if present
     */
    @Override
    public @NotNull CompletableFuture<Boolean> contains(@NotNull String key) {
        return CompletableFuture.completedFuture(map.containsKey(key));
    }

    /**
     * Returns a snapshot of all entries currently in the cache.
     *
     * @return an already-completed future containing a copy of the cache contents
     */
    @Override
    public @NotNull CompletableFuture<Map<String, V>> all() {
        return CompletableFuture.completedFuture(new HashMap<>(map));
    }
}
