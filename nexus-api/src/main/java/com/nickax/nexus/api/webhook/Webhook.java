package com.nickax.nexus.api.webhook;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * A handle to a Discord webhook URL.
 */
public interface Webhook {

    /**
     * Sends a message to the webhook.
     *
     * @param message the message
     * @return a future completing when the request finishes
     */
    @NotNull CompletableFuture<Void> send(@NotNull WebhookMessage message);
}
