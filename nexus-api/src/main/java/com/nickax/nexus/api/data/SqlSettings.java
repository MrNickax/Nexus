package com.nickax.nexus.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Connection settings for the shared SQL (MySQL + HikariCP) data source.
 *
 * @param jdbcUrl  the JDBC URL
 * @param username the user name
 * @param password the password, or {@code null} if none
 * @param poolSize the maximum Hikari pool size
 */
public record SqlSettings(@NotNull String jdbcUrl, @NotNull String username, @Nullable String password, int poolSize) {
}
