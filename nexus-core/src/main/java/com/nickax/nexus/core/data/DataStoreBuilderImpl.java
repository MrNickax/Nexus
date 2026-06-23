package com.nickax.nexus.core.data;

import com.nickax.nexus.api.data.Backend;
import com.nickax.nexus.api.data.Cache;
import com.nickax.nexus.api.data.Codec;
import com.nickax.nexus.api.data.DataStore;
import com.nickax.nexus.api.data.DataStoreBuilder;
import com.nickax.nexus.api.data.KeyMapper;
import com.nickax.nexus.api.data.MongoSettings;
import com.nickax.nexus.api.data.RedisSettings;
import com.nickax.nexus.api.data.SqlSettings;
import com.nickax.nexus.api.data.StorageProfile;
import com.nickax.nexus.api.data.WritePolicy;
import com.nickax.nexus.api.lock.LockService;
import com.nickax.nexus.core.lock.RedissonLockService;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

/**
 * Default {@link DataStoreBuilder}. Resolves Redis/Mongo/SQL clients through the
 * shared {@link ConnectionRegistry} from the settings the plugin supplies. When
 * Redis settings are present the store's per-key lock is the distributed Redisson
 * lock; otherwise it is the local lock.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public final class DataStoreBuilderImpl<K, V> implements DataStoreBuilder<K, V> {

    private final String name;
    private final Class<V> type;
    private final Path dataRoot;
    private final Executor executor;
    private final LockService localLockService;
    private final ConnectionRegistry connections;
    private final ScheduledExecutorService dataScheduler;
    private final Consumer<DataStore<?, ?>> tracker;

    private KeyMapper<K> keyMapper;
    private Codec<V> codec;
    private boolean redisCache;
    private RedisSettings redisSettings;
    private RedisSettings lockRedisSettings;
    private StorageProfile.Backend backendChoice = StorageProfile.Backend.FILE;
    private MongoSettings mongoSettings;
    private SqlSettings sqlSettings;
    private WritePolicy writePolicy = WritePolicy.writeThrough();

    /**
     * Creates a builder.
     *
     * @param name             the store name
     * @param type             the value class
     * @param dataRoot         the file-backend root folder
     * @param executor         the shared executor
     * @param localLockService the local lock service
     * @param connections      the shared connection registry
     * @param dataScheduler    the write-behind flush scheduler
     * @param tracker          a hook the built store is registered with for shutdown flushing
     */
    public DataStoreBuilderImpl(@NotNull String name, @NotNull Class<V> type, @NotNull Path dataRoot,
                                @NotNull Executor executor, @NotNull LockService localLockService,
                                @NotNull ConnectionRegistry connections,
                                @NotNull ScheduledExecutorService dataScheduler,
                                @NotNull Consumer<DataStore<?, ?>> tracker) {
        this.name = name;
        this.type = type;
        this.dataRoot = dataRoot;
        this.executor = executor;
        this.localLockService = localLockService;
        this.connections = connections;
        this.dataScheduler = dataScheduler;
        this.tracker = tracker;
    }

    /**
     * Sets the key mapper that converts typed keys to string identifiers.
     *
     * @param keyMapper the key mapper
     * @return this builder
     */
    @Override
    public @NotNull DataStoreBuilder<K, V> key(@NotNull KeyMapper<K> keyMapper) {
        this.keyMapper = keyMapper;
        return this;
    }

    /**
     * Overrides the default Gson codec with a custom one.
     *
     * @param codec the value codec
     * @return this builder
     */
    @Override
    public @NotNull DataStoreBuilder<K, V> codec(@NotNull Codec<V> codec) {
        this.codec = codec;
        return this;
    }

    /**
     * Configures an in-process {@link MemoryCache} as the hot tier (the default).
     *
     * @return this builder
     */
    @Override
    public @NotNull DataStoreBuilder<K, V> memoryCache() {
        this.redisCache = false;
        return this;
    }

    /**
     * Configures a distributed {@link RedisCache} as the hot tier. Also sets the
     * lock Redis settings to these settings unless overridden by
     * {@link #distributedLocks}.
     *
     * @param settings the Redis connection settings
     * @return this builder
     */
    @Override
    public @NotNull DataStoreBuilder<K, V> redisCache(@NotNull RedisSettings settings) {
        this.redisCache = true;
        this.redisSettings = settings;
        if (lockRedisSettings == null) lockRedisSettings = settings;
        return this;
    }

    /**
     * Uses a Redisson-backed distributed lock instead of the local one.
     *
     * @param settings the Redis connection settings for the lock service
     * @return this builder
     */
    @Override
    public @NotNull DataStoreBuilder<K, V> distributedLocks(@NotNull RedisSettings settings) {
        this.lockRedisSettings = settings;
        return this;
    }

    /**
     * Configures a file-backed durable store (the default).
     *
     * @return this builder
     */
    @Override
    public @NotNull DataStoreBuilder<K, V> fileBackend() {
        this.backendChoice = StorageProfile.Backend.FILE;
        return this;
    }

    /**
     * Configures a MongoDB-backed durable store.
     *
     * @param settings the Mongo connection settings
     * @return this builder
     */
    @Override
    public @NotNull DataStoreBuilder<K, V> mongoBackend(@NotNull MongoSettings settings) {
        this.backendChoice = StorageProfile.Backend.MONGO;
        this.mongoSettings = settings;
        return this;
    }

    /**
     * Configures a SQL-backed durable store.
     *
     * @param settings the SQL connection settings
     * @return this builder
     */
    @Override
    public @NotNull DataStoreBuilder<K, V> sqlBackend(@NotNull SqlSettings settings) {
        this.backendChoice = StorageProfile.Backend.SQL;
        this.sqlSettings = settings;
        return this;
    }

    /**
     * Applies a {@link StorageProfile} shorthand, configuring cache, backend, and
     * distributed locks in one call.
     *
     * @param profile the storage profile to apply
     * @return this builder
     */
    @Override
    public @NotNull DataStoreBuilder<K, V> storage(@NotNull StorageProfile profile) {
        RedisSettings redis = profile.redis();
        if (profile.cache() == StorageProfile.Cache.REDIS) {
            if (redis == null) {
                throw new IllegalArgumentException("storage profile cache is REDIS but no Redis settings are present");
            }
            redisCache(redis);
        } else {
            memoryCache();
        }
        switch (profile.backend()) {
            case FILE -> fileBackend();
            case MONGO -> {
                MongoSettings mongo = profile.mongo();
                if (mongo == null) {
                    throw new IllegalArgumentException("storage profile backend is MONGO but no Mongo settings are present");
                }
                mongoBackend(mongo);
            }
            case SQL -> {
                SqlSettings sql = profile.sql();
                if (sql == null) {
                    throw new IllegalArgumentException("storage profile backend is SQL but no SQL settings are present");
                }
                sqlBackend(sql);
            }
        }
        if (redis != null) {
            distributedLocks(redis);
        }
        return this;
    }

    /**
     * Sets the write policy (write-through or write-behind). Defaults to
     * write-through if not called.
     *
     * @param policy the write policy
     * @return this builder
     */
    @Override
    public @NotNull DataStoreBuilder<K, V> writePolicy(@NotNull WritePolicy policy) {
        this.writePolicy = policy;
        return this;
    }

    /**
     * Builds the {@link DataStore}. Validates the store name and key mapper, resolves
     * the cache and backend from the configured settings, and schedules a periodic
     * write-behind flush if applicable.
     *
     * @return the built store, already registered with the shutdown tracker
     * @throws IllegalArgumentException if the store name is not file-safe
     * @throws IllegalStateException    if no key mapper was configured
     */
    @Override
    public @NotNull DataStore<K, V> build() {
        if (!name.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Store name must match [A-Za-z0-9_]+ (got '"
                    + name + "'); use a file/SQL-safe name like \"myplugin_players\"");
        }
        if (keyMapper == null) {
            throw new IllegalStateException("A key mapper is required (call key(...))");
        }
        Codec<V> effectiveCodec = codec != null ? codec : new GsonCodec<>(type);

        Cache<V> cache = redisCache
                ? new RedisCache<>(connections.redisson(redisSettings).getMap("nexus:cache:" + name), effectiveCodec, executor)
                : new MemoryCache<>();

        Backend<V> backend = switch (backendChoice) {
            case FILE -> new FileBackend<>(dataRoot.resolve(name), effectiveCodec, executor);
            case MONGO -> new MongoBackend<>(connections.mongo(mongoSettings).getCollection(name, Document.class), effectiveCodec, executor);
            case SQL -> new SqlBackend<>(connections.sql(sqlSettings), "nexus_" + name, effectiveCodec, executor);
        };

        LockService lockService = lockRedisSettings != null
                ? new RedissonLockService(connections.redisson(lockRedisSettings))
                : localLockService;

        DataStore<K, V> store = new DataStoreImpl<>(keyMapper, cache, backend, lockService, writePolicy, dataScheduler);
        tracker.accept(store);
        return store;
    }
}
