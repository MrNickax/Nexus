package com.nickax.nexus.api.lang;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Renders and sends localized messages. Templates are MiniMessage strings with
 * {@code {name}} placeholders. Built via {@code nexus.lang().builder()}.
 */
public interface Lang {

    /**
     * Renders a message for an explicit locale.
     *
     * @param locale       the locale id
     * @param key          the message key
     * @param placeholders placeholders to substitute
     * @return the rendered component (the raw key if unknown)
     */
    @NotNull Component render(@NotNull String locale, @NotNull String key, @NotNull Placeholder... placeholders);

    /**
     * Resolves a message to its substituted template string for an explicit locale,
     * without parsing it into a component. Use this when the caller wants to apply its
     * own {@link com.nickax.nexus.api.text.TextFormat} to the result. Placeholder values
     * are still escaped so they cannot inject MiniMessage tags.
     *
     * @param locale       the locale id
     * @param key          the message key
     * @param placeholders placeholders to substitute
     * @return the substituted template (the raw key if unknown)
     */
    @NotNull String resolve(@NotNull String locale, @NotNull String key, @NotNull Placeholder... placeholders);

    /**
     * Renders a message for an audience's resolved locale.
     *
     * @param audience     the recipient
     * @param key          the message key
     * @param placeholders placeholders to substitute
     * @return the rendered component
     */
    @NotNull Component render(@NotNull Audience audience, @NotNull String key, @NotNull Placeholder... placeholders);

    /**
     * Renders for the audience's locale and sends it to the audience.
     *
     * @param audience     the recipient
     * @param key          the message key
     * @param placeholders placeholders to substitute
     */
    void send(@NotNull Audience audience, @NotNull String key, @NotNull Placeholder... placeholders);
}
