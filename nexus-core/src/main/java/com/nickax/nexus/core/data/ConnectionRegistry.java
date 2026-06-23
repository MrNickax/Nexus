package com.nickax.nexus.core.data;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.nickax.nexus.api.data.MongoSettings;
import com.nickax.nexus.api.data.RedisSettings;
import com.nickax.nexus.api.data.SqlSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.Credentials;
import org.redisson.config.SingleServerConfig;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lazily creates and shares backend clients, one per distinct connection setting.
 * Two stores (or plugins) that request the same {@link RedisSettings} /
 * {@link MongoSettings} connection string / {@link SqlSettings} get the same
 * underlying client, so the core opens one pool per real endpoint rather than one
 * per consumer. All created clients are closed by {@link #closeAll()} on shutdown.
 */
public final class ConnectionRegistry {

    private final Map<RedisSettings, RedissonClient> redissonClients = new ConcurrentHashMap<>();
    private final Map<String, MongoClient> mongoClients = new ConcurrentHashMap<>();
    private final Map<SqlSettings, HikariDataSource> dataSources = new ConcurrentHashMap<>();

    /**
     * Returns the shared Redisson client for the given settings, creating it on first use.
     *
     * @param settings the Redis connection settings
     * @return the shared client
     */
    public @NotNull RedissonClient redisson(@NotNull RedisSettings settings) {
        return redissonClients.computeIfAbsent(settings, this::createRedisson);
    }

    /**
     * Builds a Redisson single-server client from the settings.
     *
     * @param settings the Redis connection settings
     * @return a new client
     */
    private RedissonClient createRedisson(RedisSettings settings) {
        Config config = new Config();
        SingleServerConfig single = config.useSingleServer()
                .setAddress(settings.address())
                .setDatabase(settings.database());
        String password = settings.password();
        if (password != null) {
            // setPassword(String) is deprecated in Redisson; supply the password through a
            // credentials resolver instead (null username selects the default Redis user).
            single.setCredentialsResolver(address ->
                    CompletableFuture.completedFuture(new Credentials(null, password))
            );
        }
        return Redisson.create(config);
    }

    /**
     * Returns the database handle for the given Mongo settings; the underlying
     * {@link MongoClient} is shared per connection string.
     *
     * @param settings the Mongo connection settings
     * @return the database handle
     */
    @SuppressWarnings("resource") // the client is cached per connection string and closed in closeAll()
    public @NotNull MongoDatabase mongo(@NotNull MongoSettings settings) {
        MongoClient client = mongoClients.computeIfAbsent(settings.connectionString(), MongoClients::create);
        return client.getDatabase(settings.database());
    }

    /**
     * Returns the shared Hikari data source for the given settings, creating it on first use.
     *
     * @param settings the SQL connection settings
     * @return the shared data source
     */
    public @NotNull DataSource sql(@NotNull SqlSettings settings) {
        return dataSources.computeIfAbsent(settings, this::createDataSource);
    }

    /**
     * Builds a Hikari data source from the settings.
     *
     * @param settings the SQL connection settings
     * @return a new data source
     */
    private HikariDataSource createDataSource(SqlSettings settings) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.jdbcUrl());
        config.setUsername(settings.username());
        if (settings.password() != null) {
            config.setPassword(settings.password());
        }
        config.setMaximumPoolSize(settings.poolSize());
        config.setPoolName("nexus-sql");
        return new HikariDataSource(config);
    }

    /**
     * Closes every client created by this registry. Called on Nexus shutdown.
     */
    public void closeAll() {
        redissonClients.values().forEach(RedissonClient::shutdown);
        mongoClients.values().forEach(MongoClient::close);
        dataSources.values().forEach(HikariDataSource::close);
        redissonClients.clear();
        mongoClients.clear();
        dataSources.clear();
    }
}
