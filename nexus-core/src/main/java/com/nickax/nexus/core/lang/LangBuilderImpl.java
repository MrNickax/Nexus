package com.nickax.nexus.core.lang;

import com.nickax.nexus.api.lang.Lang;
import com.nickax.nexus.api.lang.LangBuilder;
import com.nickax.nexus.api.lang.LocaleResolver;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Default {@link LangBuilder}.
 */
public final class LangBuilderImpl implements LangBuilder {

    private String defaultLocale = "en";
    private LocaleResolver resolver;
    private final Map<String, Map<String, String>> bundles = new HashMap<>();

    /**
     * Sets the locale id used when a bundle for the audience's locale is not found.
     * Defaults to {@code "en"} if not called.
     *
     * @param locale the fallback locale id
     * @return this builder
     */
    @Override
    public @NotNull LangBuilder defaultLocale(@NotNull String locale) {
        this.defaultLocale = locale;
        return this;
    }

    /**
     * Sets a custom locale resolver. If not called, an
     * {@link AudienceLocaleResolver} backed by the default locale is used.
     *
     * @param resolver the resolver that maps an audience to a locale id
     * @return this builder
     */
    @Override
    public @NotNull LangBuilder resolver(@NotNull LocaleResolver resolver) {
        this.resolver = resolver;
        return this;
    }

    /**
     * Registers a message bundle for a locale. The map is copied defensively.
     * Calling this multiple times for the same locale replaces the previous bundle.
     *
     * @param locale   the locale id this bundle is for (e.g. {@code "es"})
     * @param messages a map from message keys to MiniMessage-formatted templates
     * @return this builder
     */
    @Override
    public @NotNull LangBuilder bundle(@NotNull String locale, @NotNull Map<String, String> messages) {
        bundles.put(locale, new HashMap<>(messages));
        return this;
    }

    /**
     * Builds the {@link Lang} instance. If no resolver was configured, installs an
     * {@link AudienceLocaleResolver} using the configured default locale.
     *
     * @return the built {@link Lang}
     */
    @Override
    public @NotNull Lang build() {
        LocaleResolver effectiveResolver = resolver != null ? resolver : new AudienceLocaleResolver(defaultLocale);
        return new StandardLang(defaultLocale, effectiveResolver, new HashMap<>(bundles));
    }
}
