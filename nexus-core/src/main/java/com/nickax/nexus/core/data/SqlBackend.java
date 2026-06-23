package com.nickax.nexus.core.data;

import com.nickax.nexus.api.data.Backend;
import com.nickax.nexus.api.data.Codec;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * SQL (MySQL / HikariCP) {@link Backend}. One table per store with columns
 * {@code (id VARCHAR(255) PRIMARY KEY, data LONGTEXT)}; values are {@link Codec}
 * encoded strings. Blocking JDBC runs on the supplied executor. The table is
 * created on construction.
 *
 * @param <V> the value type
 */
public final class SqlBackend<V> implements Backend<V> {

    private final DataSource dataSource;
    private final String table;
    private final Codec<V> codec;
    private final Executor executor;

    /**
     * Creates a SQL backend and ensures its table exists.
     *
     * @param dataSource the pooled data source
     * @param tableName  the store's table name (must be a safe identifier)
     * @param codec      the value codec
     * @param executor   the executor blocking JDBC runs on
     */
    public SqlBackend(@NotNull DataSource dataSource, @NotNull String tableName,
                      @NotNull Codec<V> codec, @NotNull Executor executor) {
        if (!tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Unsafe table name: " + tableName);
        }
        this.dataSource = dataSource;
        this.table = tableName;
        this.codec = codec;
        this.executor = executor;
        createTable();
    }

    /**
     * Executes a {@code CREATE TABLE IF NOT EXISTS} DDL statement for this backend's
     * table. Called once in the constructor on the calling thread; assumes a
     * synchronous JDBC connection is safe at startup.
     *
     * @throws IllegalStateException if the DDL fails
     */
    private void createTable() {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS " + table + " (id VARCHAR(255) PRIMARY KEY, data LONGTEXT)");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create table " + table, e);
        }
    }

    /**
     * Selects the row with the given id and decodes its {@code data} column.
     *
     * @param key the entry key
     * @return a future containing the optional decoded value
     */
    @Override
    public @NotNull CompletableFuture<Optional<V>> get(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT data FROM " + table + " WHERE id = ?")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(codec.decode(rs.getString(1))) : Optional.empty();
                }
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Upserts a row using {@code INSERT ... ON DUPLICATE KEY UPDATE} so the call
     * is idempotent on MySQL-compatible databases.
     *
     * @param key   the entry key
     * @param value the value to persist
     * @return a future that completes when the upsert is done
     */
    @Override
    public @NotNull CompletableFuture<Void> put(@NotNull String key, @NotNull V value) {
        return CompletableFuture.runAsync(() -> {
            String encoded = codec.encode(value);
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("INSERT INTO " + table
                         + " (id, data) VALUES (?, ?) ON DUPLICATE KEY UPDATE data = ?")) {
                ps.setString(1, key);
                ps.setString(2, encoded);
                ps.setString(3, encoded);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Deletes the row for {@code key}. No-ops silently when the row is absent.
     *
     * @param key the entry key to remove
     * @return a future that completes when the delete is done
     */
    @Override
    public @NotNull CompletableFuture<Void> remove(@NotNull String key) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE id = ?")) {
                ps.setString(1, key);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Returns whether a row with the given id exists in the table.
     *
     * @param key the entry key to test
     * @return a future resolving to {@code true} if the row is present
     */
    @Override
    public @NotNull CompletableFuture<Boolean> contains(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT 1 FROM " + table + " WHERE id = ?")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Fetches every row in the table and decodes each {@code data} column.
     *
     * @return a future containing all entries in the table
     */
    @Override
    public @NotNull CompletableFuture<Map<String, V>> all() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, V> result = new HashMap<>();
            try (Connection c = dataSource.getConnection();
                 Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT id, data FROM " + table)) {
                while (rs.next()) {
                    result.put(rs.getString(1), codec.decode(rs.getString(2)));
                }
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
            return result;
        }, executor);
    }
}
