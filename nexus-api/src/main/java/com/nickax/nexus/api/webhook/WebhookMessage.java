package com.nickax.nexus.api.webhook;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A webhook message: optional content plus optional embeds.
 *
 * @param content the message content, or {@code null}
 * @param embeds  the embeds (possibly empty)
 */
public record WebhookMessage(@Nullable String content, @NotNull List<Embed> embeds) {

    /**
     * Returns a new fluent builder for constructing a {@link WebhookMessage}.
     *
     * @return a new message builder
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Fluent {@link WebhookMessage} builder.
     */
    public static final class Builder {
        private String content;
        private final List<Embed> embeds = new ArrayList<>();

        /**
         * Private constructor — use {@link WebhookMessage#builder()}.
         */
        private Builder() {
        }

        /**
         * Sets the plain-text message content.
         *
         * @param content the content
         * @return this builder
         */
        public @NotNull Builder content(@NotNull String content) { this.content = content; return this; }

        /**
         * Adds an embed to this message.
         *
         * @param embed the embed to add
         * @return this builder
         */
        public @NotNull Builder embed(@NotNull Embed embed) { this.embeds.add(embed); return this; }

        /**
         * Builds and returns the configured message.
         *
         * @return the built message
         * @throws IllegalStateException if the message has neither content nor at least one embed
         */
        public @NotNull WebhookMessage build() {
            if (content == null && embeds.isEmpty()) {
                throw new IllegalStateException("A WebhookMessage must have content or at least one embed");
            }
            return new WebhookMessage(content, List.copyOf(embeds));
        }
    }
}
