package com.nickax.nexus.api.lang;

import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves the locale id to use for a recipient. The default reads the audience's
 * locale; plugins may supply one backed by their own player data.
 */
@FunctionalInterface
public interface LocaleResolver {

    /**
     * Resolves a locale id (e.g. {@code "en"}, {@code "es"}) for an audience.
     *
     * @param audience the recipient
     * @return the locale id
     */
    @NotNull String resolve(@NotNull Audience audience);
}
