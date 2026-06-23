package com.nickax.nexus.api.lang;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Builds a {@link Lang}. A default locale is required; at least one bundle should
 * be registered.
 */
public interface LangBuilder {

    /**
     * Sets the default (fallback) locale id.
     *
     * @param locale the default locale id
     * @return this builder
     */
    @NotNull LangBuilder defaultLocale(@NotNull String locale);

    /**
     * Sets a custom locale resolver (defaults to reading the audience's locale).
     * The default resolver returns the audience's bare language code.
     *
     * @param resolver the resolver
     * @return this builder
     */
    @NotNull LangBuilder resolver(@NotNull LocaleResolver resolver);

    /**
     * Registers the key→template map for a locale.
     * Calling this twice for the same locale replaces the previous bundle.
     *
     * @param locale   the locale id
     * @param messages the key→MiniMessage-template map
     * @return this builder
     */
    @NotNull LangBuilder bundle(@NotNull String locale, @NotNull Map<String, String> messages);

    /**
     * Builds and returns the configured {@link Lang} instance.
     *
     * @return the built Lang
     */
    @NotNull Lang build();
}
