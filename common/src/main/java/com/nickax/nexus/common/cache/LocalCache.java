package com.nickax.nexus.common.cache;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A local in-memory cache implementation with support for time-to-live (TTL) expiration.
 * <p>
 * This cache stores key-value pairs in concurrent maps and provides asynchronous operations
 * for all cache interactions. Expired entries are automatically cleaned up at regular intervals.
 * </p>
 *
 * @param <T> the type of values stored in the cache
 */
public class LocalCache<T> extends Cache<T> {

    private final Map<String, T> content = new ConcurrentHashMap<>();
    private final Map<String, Long> expiryTimes = new ConcurrentHashMap<>();
    private final Logger logger;
    private final ScheduledFuture<?> cleanupTask;
    private final Executor executor;

    /**
     * Constructs a new LocalCache with the specified logger and cleanup interval.
     *
     * @param logger          the logger for recording cache operations and errors
     * @param cleanupInterval the interval at which expired entries are removed from the cache
     */
    public LocalCache(Logger logger, Duration cleanupInterval) {
        this.logger = logger;
        this.executor = LocalCacheExecutor.getCacheExecutor();
        ScheduledExecutorService cleanupExecutor = LocalCacheExecutor.getCleanupExecutor();
        cleanupTask = cleanupExecutor.scheduleAtFixedRate(
                this::cleanupExpiredEntries,
                cleanupInterval.toSeconds(),
                cleanupInterval.toSeconds(),
                TimeUnit.SECONDS
        );
    }

    /**
     * Retrieves the value associated with the specified key asynchronously.
     *
     * @param key the key whose associated value is to be returned
     * @return a CompletableFuture containing the value associated with the key, or null if not found
     */
    @Override
    public CompletableFuture<T> get(String key) {
        return supplyAsync("get", key, () -> content.get(key));
    }

    /**
     * Retrieves the value associated with the specified key synchronously.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the key, or null if not found
     */
    public T getSync(String key) {
        return content.get(key);
    }

    /**
     * Updates the value associated with the specified key using the provided update function.
     * <p>
     * If the update function returns null, the entry is removed from the cache.
     * </p>
     *
     * @param key            the key whose value is to be updated
     * @param updateFunction the function to apply to the current value
     * @return a CompletableFuture containing the updated value, or null if the entry was removed
     */
    @Override
    public CompletableFuture<T> update(String key, Function<T, T> updateFunction) {
        return supplyAsync("update", key, () -> {
            T currentValue = content.get(key);
            T updatedValue = updateFunction.apply(currentValue);

            if (updatedValue != null) {
                content.put(key, updatedValue);
                return updatedValue;
            }

            content.remove(key);
            expiryTimes.remove(key);

            return null;
        });
    }

    /**
     * Associates the specified value with the specified key with a time-to-live.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @param ttl   the duration after which the entry should expire
     * @return a CompletableFuture containing the previous value associated with the key, or null if none
     */
    @Override
    public CompletableFuture<T> put(String key, T value, Duration ttl) {
        return supplyAsync("put(ttl)", key, () -> {
            expiryTimes.put(key, System.currentTimeMillis() + ttl.toMillis());
            return content.put(key, value);
        });
    }

    /**
     * Associates the specified value with the specified key without a time-to-live.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return a CompletableFuture containing the previous value associated with the key, or null if none
     */
    @Override
    public CompletableFuture<T> put(String key, T value) {
        return supplyAsync("put", key, () -> content.put(key, value));
    }

    /**
     * Associates the specified value with the specified key with a time-to-live if the key is not already present.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @param ttl   the duration after which the entry should expire
     * @return a CompletableFuture containing the previous value associated with the key, or null if the key was absent
     */
    @Override
    public CompletableFuture<T> putIfAbsent(String key, T value, Duration ttl) {
        return supplyAsync("putIfAbsent(ttl)", key, () -> {
            T currentValue = content.get(key);

            if (currentValue == null) {
                expiryTimes.put(key, System.currentTimeMillis() + ttl.toMillis());
                return content.put(key, value);
            }

            return null;
        });
    }

    /**
     * Associates the specified value with the specified key if the key is not already present.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return a CompletableFuture containing the previous value associated with the key, or null if the key was absent
     */
    @Override
    public CompletableFuture<T> putIfAbsent(String key, T value) {
        return supplyAsync("putIfAbsent", key, () -> content.putIfAbsent(key, value));
    }

    /**
     * Removes the mapping for the specified key from the cache.
     *
     * @param key the key whose mapping is to be removed
     * @return a CompletableFuture containing the previous value associated with the key, or null if none
     */
    @Override
    public CompletableFuture<T> remove(String key) {
        return supplyAsync("remove", key, () -> {
            expiryTimes.remove(key);
            return content.remove(key);
        });
    }

    /**
     * Retrieves all keys currently stored in the cache.
     *
     * @return a CompletableFuture containing a set of all keys in the cache
     */
    @Override
    public CompletableFuture<Set<String>> getKeys() {
        return supplyAsync("getKeys", null, content::keySet);
    }

    /**
     * Retrieves all key-value pairs currently stored in the cache.
     *
     * @return a CompletableFuture containing an immutable copy of all entries in the cache
     */
    @Override
    public CompletableFuture<Map<String, T>> getAll() {
        return supplyAsync("getAll", null, () -> Map.copyOf(content));
    }

    /**
     * Associates all entries from the specified map with the cache with a time-to-live.
     *
     * @param entries the map containing key-value pairs to be stored
     * @param ttl     the duration after which all entries should expire
     * @return a CompletableFuture that completes when all entries have been stored
     */
    @Override
    public CompletableFuture<Void> putAll(Map<String, T> entries, Duration ttl) {
        long expiryTime = System.currentTimeMillis() + ttl.toMillis();
        return runAsync("putAll(ttl)", null, () -> entries.forEach((key, value) -> {
            expiryTimes.put(key, expiryTime);
            content.put(key, value);
        }));
    }

    /**
     * Associates all entries from the specified map with the cache without a time-to-live.
     *
     * @param entries the map containing key-value pairs to be stored
     * @return a CompletableFuture that completes when all entries have been stored
     */
    @Override
    public CompletableFuture<Void> putAll(Map<String, T> entries) {
        return runAsync("putAll", null, () -> content.putAll(entries));
    }

    /**
     * Removes all mappings for the specified keys from the cache.
     *
     * @param keys the list of keys whose mappings are to be removed
     * @return a CompletableFuture that completes when all entries have been removed
     */
    @Override
    public CompletableFuture<Void> removeAll(List<String> keys) {
        return runAsync("removeAll", null, () -> keys.forEach(key -> {
            content.remove(key);
            expiryTimes.remove(key);
        }));
    }

    /**
     * Retrieves the remaining time-to-live for the specified key.
     *
     * @param key the key whose TTL is to be retrieved
     * @return a CompletableFuture containing the remaining TTL, or null if the key has no expiration or doesn't exist
     */
    @Override
    public CompletableFuture<@Nullable Duration> getTTL(String key) {
        return supplyAsync("getTTL", key, () -> {
            Long expiry = expiryTimes.get(key);
            return expiry == null ? null : Duration.ofMillis(expiry - System.currentTimeMillis());
        });
    }

    /**
     * Sets or updates the time-to-live for the specified key.
     *
     * @param key the key whose TTL is to be set
     * @param ttl the duration after which the entry should expire
     * @return a CompletableFuture that completes when the TTL has been updated
     */
    @Override
    public CompletableFuture<Void> setTTL(String key, Duration ttl) {
        return runAsync("setTTL", key, () ->
                expiryTimes.put(key, System.currentTimeMillis() + ttl.toMillis())
        );
    }

    /**
     * Shuts down the cache, canceling the cleanup task and clearing all stored data.
     * <p>
     * This method should be called when the cache is no longer needed to free up resources.
     * </p>
     */
    @Override
    public void shutdown() {
        try {
            cleanupTask.cancel(true);
            content.clear();
            expiryTimes.clear();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "LocalCache shutdown failed", e);
        }
    }

    /**
     * Executes a task asynchronously that returns a result.
     *
     * @param operation the name of the operation being performed (for logging)
     * @param key       the key associated with the operation, or null if not applicable
     * @param task      the task to execute
     * @param <R>       the type of the result
     * @return a CompletableFuture containing the result of the task
     */
    private <R> CompletableFuture<R> supplyAsync(String operation, String key, Callable<R> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.log(Level.SEVERE, formatMessage(operation, key), unwrap(throwable));
            }
        });
    }

    /**
     * Executes a task asynchronously that does not return a result.
     *
     * @param operation the name of the operation being performed (for logging)
     * @param key       the key associated with the operation, or null if not applicable
     * @param task      the task to execute
     * @return a CompletableFuture that completes when the task finishes
     */
    private CompletableFuture<Void> runAsync(String operation, String key, Runnable task) {
        return CompletableFuture.runAsync(task, executor).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.log(Level.SEVERE, formatMessage(operation, key), unwrap(throwable));
            }
        });
    }

    /**
     * Formats an error message for logging purposes.
     *
     * @param operation the name of the operation that failed
     * @param key       the key associated with the operation, or null if not applicable
     * @return a formatted error message
     */
    private String formatMessage(String operation, String key) {
        return key == null
                ? "LocalCache " + operation + " failed"
                : "LocalCache " + operation + " failed for key: " + key;
    }

    /**
     * Unwraps a CompletionException to retrieve the underlying cause.
     *
     * @param throwable the throwable to unwrap
     * @return the underlying cause if the throwable is a CompletionException, otherwise the original throwable
     */
    private Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException completionException && completionException.getCause() != null) {
            return completionException.getCause();
        }
        return throwable;
    }

    /**
     * Removes expired entries from the cache.
     * <p>
     * This method is scheduled to run periodically to clean up entries that have exceeded their TTL.
     * </p>
     */
    private void cleanupExpiredEntries() {
        try {
            long now = System.currentTimeMillis();

            Iterator<Map.Entry<String, Long>> iterator = expiryTimes.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Long> entry = iterator.next();
                if (entry.getValue() < now) {
                    String key = entry.getKey();
                    iterator.remove();
                    content.remove(key);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "LocalCache cleanup task failed", e);
        }
    }
}