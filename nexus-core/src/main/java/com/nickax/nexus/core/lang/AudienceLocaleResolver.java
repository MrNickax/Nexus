package com.nickax.nexus.core.lang;

import com.nickax.nexus.api.lang.LocaleResolver;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Default {@link LocaleResolver}: uses the audience's {@code Identity.LOCALE}
 * pointer (its client language), falling back to a default locale id.
 * Returns the bare ISO language code (e.g. "es" for a client locale of es_ES);
 * register bundles under bare language codes, or supply a custom resolver for
 * region-specific keys.
 */
public final class AudienceLocaleResolver implements LocaleResolver {

    private final String defaultLocale;

    /**
     * Creates the resolver with the given fallback locale.
     *
     * @param defaultLocale the locale id to use when the audience has no locale pointer
     */
    public AudienceLocaleResolver(@NotNull String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    /**
     * Resolves the audience's locale to a bare ISO 639-1 language code (e.g. "es"
     * from {@code es_ES}), or the configured default when the audience provides none.
     *
     * @param audience the audience to resolve a locale for
     * @return the ISO language code, or the default locale id
     */
    @Override
    public @NotNull String resolve(@NotNull Audience audience) {
        Locale locale = audience.getOrDefault(Identity.LOCALE, null);
        return locale != null ? locale.getLanguage() : defaultLocale;
    }
}
