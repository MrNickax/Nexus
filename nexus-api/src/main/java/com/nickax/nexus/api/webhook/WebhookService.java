package com.nickax.nexus.api.webhook;

import org.jetbrains.annotations.NotNull;

/**
 * Creates {@link Webhook}s. Obtain via {@code nexus.webhooks()}.
 */
public interface WebhookService {

    /**
     * Creates a webhook handle for a URL.
     *
     * @param url the Discord webhook URL
     * @return the webhook
     */
    @NotNull Webhook create(@NotNull String url);
}
