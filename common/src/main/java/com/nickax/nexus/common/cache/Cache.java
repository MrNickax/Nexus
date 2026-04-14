package com.nickax.nexus.common.cache;

import com.nickax.nexus.common.repository.BaseRepository;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for cache implementations that extends BaseRepository functionality
 * with time-to-live (TTL) support for cached entries.
 *
 * @param <T> the type of values stored in the cache
 */
public abstract class Cache<T> implements BaseRepository<T> {

    /**
     * Stores a key-value pair in the cache with a specified time-to-live.
     *
     * @param key   the key under which to store the value
     * @param value the value to store
     * @param ttl   the duration after which the entry should expire
     * @return a CompletableFuture containing the previous value associated with the key,
     * or null if there was no previous value
     */
    public abstract CompletableFuture<T> put(String key, T value, Duration ttl);

    /**
     * Stores a key-value pair in the cache only if the key is not already present,
     * with a specified time-to-live.
     *
     * @param key   the key under which to store the value
     * @param value the value to store
     * @param ttl   the duration after which the entry should expire
     * @return a CompletableFuture containing the existing value if the key was already present,
     * or null if the key was absent and the value was stored
     */
    public abstract CompletableFuture<T> putIfAbsent(String key, T value, Duration ttl);

    /**
     * Stores multiple key-value pairs in the cache with a specified time-to-live
     * applied to all entries.
     *
     * @param entries a map of key-value pairs to store
     * @param ttl     the duration after which all entries should expire
     * @return a CompletableFuture that completes when all entries have been stored
     */
    public abstract CompletableFuture<Void> putAll(Map<String, T> entries, Duration ttl);

    /**
     * Retrieves the remaining time-to-live for a cached entry.
     *
     * @param key the key of the entry to check
     * @return a CompletableFuture containing the remaining TTL duration,
     * or null if the key does not exist or has no TTL set
     */
    public abstract CompletableFuture<@Nullable Duration> getTTL(String key);

    /**
     * Sets or updates the time-to-live for an existing cached entry.
     *
     * @param key the key of the entry to update
     * @param ttl the new duration after which the entry should expire
     * @return a CompletableFuture that completes when the TTL has been updated
     */
    public abstract CompletableFuture<Void> setTTL(String key, Duration ttl);
}