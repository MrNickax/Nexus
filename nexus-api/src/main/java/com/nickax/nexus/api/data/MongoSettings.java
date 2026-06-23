package com.nickax.nexus.api.data;

import org.jetbrains.annotations.NotNull;

/**
 * Connection settings for the shared MongoDB client.
 *
 * @param connectionString the MongoDB connection string (e.g. {@code mongodb://host:27017})
 * @param database         the database name
 */
public record MongoSettings(@NotNull String connectionString, @NotNull String database) {
}
