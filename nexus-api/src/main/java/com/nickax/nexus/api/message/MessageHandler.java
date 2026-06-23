package com.nickax.nexus.api.message;

import org.jetbrains.annotations.NotNull;

/**
 * Receives messages on a {@link Channel}.
 *
 * @param <T> the payload type
 */
@FunctionalInterface
public interface MessageHandler<T> {

    /**
     * Handles a received message.
     *
     * @param message the payload
     * @param context the message context (source node, self-flag)
     */
    void handle(@NotNull T message, @NotNull MessageContext context);
}
