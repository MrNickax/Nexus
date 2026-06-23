package com.nickax.nexus.api.message;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Cross-server messaging over Redis. Requires Redis configured. Obtain via
 * {@code nexus.messaging()}.
 */
public interface Messaging {

    /**
     * Gets (or creates) a typed channel.
     *
     * @param name the channel name
     * @param type the payload class (for Gson decoding)
     * @param <T>  the payload type
     * @return the channel
     */
    <T> @NotNull Channel<T> channel(@NotNull String name, @NotNull Class<T> type);

    /**
     * Runs an action at most once across the whole network for the given operation
     * id, by reserving it in Redis with a TTL. Use for value-moving operations.
     *
     * @param operationId a unique id for the operation
     * @param ttl         how long the reservation is held
     * @param action      the action to run if this node wins the reservation
     * @return a future of whether the action ran (i.e. this node won)
     */
    @NotNull CompletableFuture<Boolean> once(@NotNull String operationId, @NotNull Duration ttl, @NotNull Runnable action);
}
