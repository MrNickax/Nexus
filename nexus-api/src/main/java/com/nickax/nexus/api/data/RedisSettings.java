package com.nickax.nexus.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Connection settings for the shared Redis (Redisson) client. Sourced from
 * Nexus configuration. When absent, Redis-backed cache and distributed locks
 * are unavailable and the local equivalents are used.
 *
 * @param host     the Redis host
 * @param port     the Redis port
 * @param password the password, or {@code null} if none
 * @param database the Redis database index
 */
public record RedisSettings(@NotNull String host, int port, @Nullable String password, int database) {

    /**
     * Returns the {@code redis://host:port} address string used to configure the
     * Redisson client.
     *
     * @return the formatted Redisson address
     */
    public @NotNull String address() {
        return "redis://" + host + ":" + port;
    }
}
