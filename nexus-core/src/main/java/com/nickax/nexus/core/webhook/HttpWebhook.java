package com.nickax.nexus.core.webhook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nickax.nexus.api.webhook.Embed;
import com.nickax.nexus.api.webhook.Webhook;
import com.nickax.nexus.api.webhook.WebhookMessage;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * {@link Webhook} over the JDK {@link HttpClient}, posting the Discord webhook
 * JSON shape. The client is shared and owned by Nexus.
 */
public final class HttpWebhook implements Webhook {

    private final String url;
    private final HttpClient client;

    /**
     * Creates a webhook.
     *
     * @param url    the webhook URL
     * @param client the shared HTTP client
     */
    public HttpWebhook(@NotNull String url, @NotNull HttpClient client) {
        this.url = url;
        this.client = client;
    }

    /**
     * Serialises the message to the Discord webhook JSON format and POSTs it. If the
     * HTTP response status is 3xx or above the future fails with a
     * {@link RuntimeException} containing the status code.
     *
     * @param message the message to send
     * @return a future that completes when a 2xx response is received
     */
    @Override
    public @NotNull CompletableFuture<Void> send(@NotNull WebhookMessage message) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(toJson(message), StandardCharsets.UTF_8))
                    .build();
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        return client.sendAsync(request, HttpResponse.BodyHandlers.discarding()).thenApply(response -> {
            if (response.statusCode() >= 300) {
                throw new RuntimeException("Webhook POST failed: HTTP " + response.statusCode());
            }
            return null;
        });
    }

    /**
     * Converts a {@link WebhookMessage} to a Discord-compatible JSON body string.
     * Only {@code content} and {@code embeds} (with {@code title}, {@code description},
     * and {@code color}) are serialised; other Discord fields are not yet supported.
     *
     * @param message the message to serialise
     * @return the JSON body string
     */
    private static String toJson(WebhookMessage message) {
        JsonObject root = new JsonObject();
        if (message.content() != null) {
            root.addProperty("content", message.content());
        }
        if (!message.embeds().isEmpty()) {
            JsonArray embeds = new JsonArray();
            for (Embed embed : message.embeds()) {
                JsonObject e = new JsonObject();
                if (embed.title() != null) {
                    e.addProperty("title", embed.title());
                }
                if (embed.description() != null) {
                    e.addProperty("description", embed.description());
                }
                e.addProperty("color", embed.color());
                embeds.add(e);
            }
            root.add("embeds", embeds);
        }
        return root.toString();
    }
}
