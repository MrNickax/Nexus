package com.nickax.nexus.api.data;

import com.nickax.nexus.api.config.ConfigSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A plugin's storage choice: which backend and cache to use and the connection
 * settings for each. Built directly or read from a config section with
 * {@link #from(ConfigSection)}, so a plugin configures its storage entirely in its
 * own config file.
 */
public final class StorageProfile {

    private final Backend backend;
    private final Cache cache;
    private final MongoSettings mongo;
    private final SqlSettings sql;
    private final RedisSettings redis;

    /**
     * Constructs a new StorageProfile.
     *
     * @param backend the durable backend kind
     * @param cache   the cache tier kind
     * @param mongo   the MongoDB connection settings, or {@code null} if not used
     * @param sql     the SQL connection settings, or {@code null} if not used
     * @param redis   the Redis connection settings, or {@code null} if not used
     */
    private StorageProfile(Backend backend, Cache cache, MongoSettings mongo, SqlSettings sql, RedisSettings redis) {
        this.backend = backend;
        this.cache = cache;
        this.mongo = mongo;
        this.sql = sql;
        this.redis = redis;
    }

    /**
     * Returns the durable backend kind.
     *
     * @return the backend kind
     */
    public @NotNull Backend backend() {
        return backend;
    }

    /**
     * Returns the cache tier kind.
     *
     * @return the cache kind
     */
    public @NotNull Cache cache() {
        return cache;
    }

    /**
     * Returns the MongoDB connection settings, if this profile uses Mongo.
     *
     * @return the Mongo settings, or {@code null} if not configured
     */
    public @Nullable MongoSettings mongo() {
        return mongo;
    }

    /**
     * Returns the SQL connection settings, if this profile uses SQL.
     *
     * @return the SQL settings, or {@code null} if not configured
     */
    public @Nullable SqlSettings sql() {
        return sql;
    }

    /**
     * Returns the Redis connection settings, used for a Redis cache and/or
     * distributed locks.
     *
     * @return the Redis settings, or {@code null} if not configured
     */
    public @Nullable RedisSettings redis() {
        return redis;
    }

    /**
     * Reads a profile from a config section. Recognised keys: {@code backend}
     * (file|mongo|sql, default file), {@code cache} (memory|redis, default memory),
     * and the connection sub-sections {@code mongo}, {@code sql}, {@code redis}.
     *
     * @param section the storage config section
     * @return the parsed profile
     */
    public static @NotNull StorageProfile from(@NotNull ConfigSection section) {
        Backend backend = parseBackend(section.getString("backend", "file"));
        Cache cache = parseCache(section.getString("cache", "memory"));

        MongoSettings mongo = null;
        ConfigSection mongoSection = section.getSection("mongo");
        if (mongoSection != null) {
            mongo = new MongoSettings(
                    mongoSection.getString("connection-string", "mongodb://localhost:27017"),
                    mongoSection.getString("database", "nexus"));
        }

        SqlSettings sql = null;
        ConfigSection sqlSection = section.getSection("sql");
        if (sqlSection != null) {
            String pw = sqlSection.getString("password", "");
            sql = new SqlSettings(
                    sqlSection.getString("jdbc-url", "jdbc:mysql://localhost:3306/nexus"),
                    sqlSection.getString("username", "root"),
                    pw == null || pw.isEmpty() ? null : pw,
                    sqlSection.getInt("pool-size", 10));
        }

        RedisSettings redis = null;
        ConfigSection redisSection = section.getSection("redis");
        if (redisSection != null) {
            String pw = redisSection.getString("password", "");
            redis = new RedisSettings(
                    redisSection.getString("host", "localhost"),
                    redisSection.getInt("port", 6379),
                    pw == null || pw.isEmpty() ? null : pw,
                    redisSection.getInt("database", 0));
        }

        if (backend == Backend.MONGO && mongo == null) {
            throw new IllegalArgumentException("backend is 'mongo' but no 'mongo' section is configured");
        }
        if (backend == Backend.SQL && sql == null) {
            throw new IllegalArgumentException("backend is 'sql' but no 'sql' section is configured");
        }
        if (cache == Cache.REDIS && redis == null) {
            throw new IllegalArgumentException("cache is 'redis' but no 'redis' section is configured");
        }

        return new StorageProfile(backend, cache, mongo, sql, redis);
    }

    /**
     * Parses a backend kind from a raw config string (case-insensitive).
     *
     * @param raw the raw config value (e.g. {@code "file"}, {@code "mongo"}, {@code "sql"})
     * @return the matching backend kind
     * @throws IllegalArgumentException if {@code raw} does not match any known backend
     */
    private static Backend parseBackend(String raw) {
        try {
            return Backend.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid storage backend '" + raw + "' — expected file, mongo or sql");
        }
    }

    /**
     * Parses a cache tier kind from a raw config string (case-insensitive).
     *
     * @param raw the raw config value (e.g. {@code "memory"}, {@code "redis"})
     * @return the matching cache kind
     * @throws IllegalArgumentException if {@code raw} does not match any known cache tier
     */
    private static Cache parseCache(String raw) {
        try {
            return Cache.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid storage cache '" + raw + "' — expected memory or redis");
        }
    }

    /**
     * Builds a file-backend, memory-cache profile (no external connections).
     *
     * @return a local profile
     */
    public static @NotNull StorageProfile local() {
        return new StorageProfile(Backend.FILE, Cache.MEMORY, null, null, null);
    }

    /**
     * The durable backend kind.
     */
    public enum Backend {

        /** Stores data in flat files under the Nexus data folder. */
        FILE,

        /** Stores data in a MongoDB collection. */
        MONGO,

        /** Stores data in a SQL table (MySQL via HikariCP). */
        SQL
    }

    /**
     * The cache tier kind.
     */
    public enum Cache {

        /** Keeps cached values in the JVM heap only. */
        MEMORY,

        /** Keeps cached values in a shared Redis instance. */
        REDIS
    }
}
