package com.nickax.nexus.api.data;

import org.jetbrains.annotations.NotNull;

/**
 * Fluent builder for a {@link DataStore}. Obtain one from {@link DataService}.
 * A key mapper is required; cache and backend default to in-memory and file
 * respectively; the default write policy is write-through.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public interface DataStoreBuilder<K, V> {

    /**
     * Sets the key mapper (required). For the file backend, the key's string form
     * must be a safe single-segment file name.
     *
     * @param keyMapper maps typed keys to string keys
     * @return this builder
     */
    @NotNull DataStoreBuilder<K, V> key(@NotNull KeyMapper<K> keyMapper);

    /**
     * Overrides the value codec (defaults to a Gson codec for the value type).
     *
     * @param codec the codec
     * @return this builder
     */
    @NotNull DataStoreBuilder<K, V> codec(@NotNull Codec<V> codec);

    /**
     * Uses an in-memory cache as the hot tier (the default).
     *
     * @return this builder
     */
    @NotNull DataStoreBuilder<K, V> memoryCache();

    /**
     * Uses a file backend (under the Nexus data folder) as the durable tier
     * (the default).
     *
     * @return this builder
     */
    @NotNull DataStoreBuilder<K, V> fileBackend();

    /**
     * Uses the distributed Redis cache as the hot tier. The underlying Redisson
     * client is resolved through the shared connection registry.
     *
     * @param settings the Redis connection settings
     * @return this builder
     */
    @NotNull DataStoreBuilder<K, V> redisCache(@NotNull RedisSettings settings);

    /**
     * Uses the MongoDB backend as the durable tier. The underlying client is
     * resolved through the shared connection registry.
     * Cross-server update() safety requires a distributed lock — call redisCache(...) or distributedLocks(...) as well.
     *
     * @param settings the Mongo connection settings
     * @return this builder
     */
    @NotNull DataStoreBuilder<K, V> mongoBackend(@NotNull MongoSettings settings);

    /**
     * Uses the SQL (MySQL/Hikari) backend as the durable tier. The underlying
     * data source is resolved through the shared connection registry.
     * Cross-server update() safety requires a distributed lock — call redisCache(...) or distributedLocks(...) as well.
     *
     * @param settings the SQL connection settings
     * @return this builder
     */
    @NotNull DataStoreBuilder<K, V> sqlBackend(@NotNull SqlSettings settings);

    /**
     * Uses a Redis-backed distributed lock for {@code update()} on the given Redis,
     * independent of the cache choice. Call this (or {@link #redisCache(RedisSettings)})
     * when concurrent updates may come from multiple servers; otherwise updates are
     * guarded only within this JVM.
     *
     * @param settings the Redis connection settings
     * @return this builder
     */
    @NotNull DataStoreBuilder<K, V> distributedLocks(@NotNull RedisSettings settings);

    /**
     * Applies backend, cache, and their connection settings from a single profile.
     * Equivalent to calling the individual {@code redisCache}/{@code mongoBackend}/
     * {@code sqlBackend}/{@code fileBackend}/{@code memoryCache} methods with the
     * settings contained in the profile.
     *
     * @param profile the storage profile
     * @return this builder
     */
    @NotNull DataStoreBuilder<K, V> storage(@NotNull StorageProfile profile);

    /**
     * Sets the write policy (defaults to write-through).
     *
     * @param policy the write policy
     * @return this builder
     */
    @NotNull DataStoreBuilder<K, V> writePolicy(@NotNull WritePolicy policy);

    /**
     * Builds the data store.
     *
     * @return the built store
     */
    @NotNull DataStore<K, V> build();
}
