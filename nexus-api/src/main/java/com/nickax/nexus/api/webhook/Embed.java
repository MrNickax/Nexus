package com.nickax.nexus.api.webhook;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A Discord webhook embed.
 *
 * @param title       the title, or {@code null}
 * @param description the description, or {@code null}
 * @param color       the decimal RGB color
 */
public record Embed(@Nullable String title, @Nullable String description, int color) {

    /**
     * Returns a new fluent builder for constructing an {@link Embed}.
     *
     * @return a new embed builder
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Fluent {@link Embed} builder.
     */
    public static final class Builder {
        private String title;
        private String description;
        private int color;

        /**
         * Private constructor — use {@link Embed#builder()}.
         */
        private Builder() {
        }

        /**
         * Sets the embed title.
         *
         * @param title the title
         * @return this builder
         */
        public @NotNull Builder title(@NotNull String title) { this.title = title; return this; }

        /**
         * Sets the embed description.
         *
         * @param description the description
         * @return this builder
         */
        public @NotNull Builder description(@NotNull String description) { this.description = description; return this; }

        /**
         * Sets the embed accent color as a decimal RGB value (e.g. {@code 0x00FF00}).
         *
         * @param color the decimal RGB color
         * @return this builder
         */
        public @NotNull Builder color(int color) { this.color = color; return this; }

        /**
         * Builds and returns the configured embed.
         *
         * @return the built embed
         */
        public @NotNull Embed build() { return new Embed(title, description, color); }
    }
}
