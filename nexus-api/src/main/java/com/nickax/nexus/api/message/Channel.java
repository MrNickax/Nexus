package com.nickax.nexus.api.message;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * A typed cross-server pub/sub channel.
 *
 * @param <T> the payload type
 */
public interface Channel<T> {

    /**
     * Publishes a payload to all subscribers (including, potentially, this node).
     *
     * @param payload the payload
     * @return a future completing when published
     */
    @NotNull CompletableFuture<Void> publish(@NotNull T payload);

    /**
     * Subscribes a handler. Handlers can check {@link MessageContext#fromSelf()} to
     * ignore their own messages.
     *
     * @param handler the handler
     * @return a subscription handle
     */
    @NotNull Subscription subscribe(@NotNull MessageHandler<T> handler);
}
