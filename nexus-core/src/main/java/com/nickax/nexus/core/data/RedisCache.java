package com.nickax.nexus.core.data;

import com.nickax.nexus.api.data.Cache;
import com.nickax.nexus.api.data.Codec;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Distributed {@link Cache} over a Redisson {@link RMap}. Values are encoded to
 * strings by the {@link Codec} (uniform with the file backend), so the same data
 * is shareable across servers. Decode/transform steps run on the supplied
 * executor to avoid blocking Redisson's Netty threads.
 *
 * @param <V> the value type
 */
public final class RedisCache<V> implements Cache<V> {

    private final RMap<String, String> map;
    private final Codec<V> codec;
    private final Executor executor;

    /**
     * Creates a Redis cache over the given map.
     *
     * @param map      the Redisson map (one per store)
     * @param codec    the value codec
     * @param executor the executor decode/transform steps run on
     */
    public RedisCache(@NotNull RMap<String, String> map, @NotNull Codec<V> codec, @NotNull Executor executor) {
        this.map = map;
        this.codec = codec;
        this.executor = executor;
    }

    /**
     * Fetches the raw string from Redis and decodes it on the executor thread.
     *
     * @param key the cache key
     * @return a future containing the optional decoded value
     */
    @Override
    public @NotNull CompletableFuture<Optional<V>> get(@NotNull String key) {
        return map.getAsync(key).toCompletableFuture()
                .thenApplyAsync(raw -> raw == null ? Optional.empty() : Optional.of(codec.decode(raw)), executor);
    }

    /**
     * Encodes the value and writes it to Redis using a non-returning fast put.
     *
     * @param key   the cache key
     * @param value the value to cache
     * @return a future that completes when the write is acknowledged
     */
    @Override
    public @NotNull CompletableFuture<Void> put(@NotNull String key, @NotNull V value) {
        return map.fastPutAsync(key, codec.encode(value)).toCompletableFuture()
                .thenApply(ignored -> null);
    }

    /**
     * Removes the entry from Redis using a non-returning fast remove.
     *
     * @param key the cache key to evict
     * @return a future that completes when the removal is acknowledged
     */
    @Override
    public @NotNull CompletableFuture<Void> remove(@NotNull String key) {
        return map.fastRemoveAsync(key).toCompletableFuture().thenApply(ignored -> null);
    }

    /**
     * Returns whether the key exists in the Redis map.
     *
     * @param key the cache key to test
     * @return a future resolving to {@code true} if the key is present
     */
    @Override
    public @NotNull CompletableFuture<Boolean> contains(@NotNull String key) {
        return map.containsKeyAsync(key).toCompletableFuture();
    }

    /**
     * Reads the entire Redis map and decodes each value on the executor thread.
     *
     * @return a future containing all decoded entries
     */
    @Override
    public @NotNull CompletableFuture<Map<String, V>> all() {
        return map.readAllMapAsync().toCompletableFuture().thenApplyAsync(raw -> {
            Map<String, V> result = new HashMap<>();
            raw.forEach((k, v) -> result.put(k, codec.decode(v)));
            return result;
        }, executor);
    }
}
