package com.nickax.nexus.core.webhook;

import com.nickax.nexus.api.webhook.Webhook;
import com.nickax.nexus.api.webhook.WebhookService;
import org.jetbrains.annotations.NotNull;

import java.net.http.HttpClient;

/**
 * Default {@link WebhookService} backed by a shared {@link HttpClient}.
 */
public final class WebhookServiceImpl implements WebhookService {

    private final HttpClient client;

    /**
     * Creates the service.
     *
     * @param client the shared HTTP client
     */
    public WebhookServiceImpl(@NotNull HttpClient client) {
        this.client = client;
    }

    /**
     * Creates a webhook that POSTs to the given URL using the shared HTTP client.
     *
     * @param url the full webhook URL
     * @return the webhook
     */
    @Override
    public @NotNull Webhook create(@NotNull String url) {
        return new HttpWebhook(url, client);
    }
}
