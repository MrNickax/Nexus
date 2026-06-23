package com.nickax.nexus.core.lang;

import com.nickax.nexus.api.lang.Lang;
import com.nickax.nexus.api.lang.LocaleResolver;
import com.nickax.nexus.api.lang.Placeholder;
import com.nickax.nexus.api.text.Text;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Default {@link Lang}. Looks up a template by (locale → default locale → raw key),
 * substitutes {@code {name}} placeholders, and parses the result as MiniMessage.
 */
public final class StandardLang implements Lang {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final String defaultLocale;
    private final LocaleResolver resolver;
    private final Map<String, Map<String, String>> bundles;

    /**
     * Package-private constructor; use {@link LangBuilderImpl#build()} instead.
     *
     * @param defaultLocale the fallback locale id
     * @param resolver      maps an audience to a locale id
     * @param bundles       locale-to-messages map; the outer map is owned by this instance
     */
    StandardLang(@NotNull String defaultLocale, @NotNull LocaleResolver resolver, @NotNull Map<String, Map<String, String>> bundles) {
        this.defaultLocale = defaultLocale;
        this.resolver = resolver;
        this.bundles = bundles;
    }

    /**
     * Looks up the MiniMessage template for a key in the given locale, falling back
     * to the default locale and finally returning the bare key when no bundle has it.
     *
     * @param locale the target locale id
     * @param key    the message key
     * @return the template string, or {@code key} if no bundle has it
     */
    private String template(String locale, String key) {
        Map<String, String> bundle = bundles.get(locale);
        if (bundle != null && bundle.containsKey(key)) {
            return bundle.get(key);
        }
        Map<String, String> fallback = bundles.get(defaultLocale);
        if (fallback != null && fallback.containsKey(key)) {
            return fallback.get(key);
        }
        return key;
    }

    /**
     * Replaces {@code {name}} tokens in {@code template} with placeholder values,
     * escaping each value's MiniMessage tags so user-supplied text cannot inject
     * formatting.
     *
     * @param template     the MiniMessage template string
     * @param placeholders the placeholders to substitute
     * @return the template with all tokens replaced
     */
    private static String substitute(String template, Placeholder[] placeholders) {
        String result = template;
        for (Placeholder placeholder : placeholders) {
            String safe = MINI_MESSAGE.escapeTags(placeholder.value());
            result = result.replace("{" + placeholder.name() + "}", safe);
        }
        return result;
    }

    /**
     * Renders a message for an explicit locale: looks up the template, substitutes
     * placeholders, and parses the result as MiniMessage.
     *
     * @param locale       the target locale id
     * @param key          the message key
     * @param placeholders placeholder values to substitute
     * @return the rendered component
     */
    @Override
    public @NotNull Component render(@NotNull String locale, @NotNull String key,
                                     @NotNull Placeholder... placeholders) {
        return Text.of(resolve(locale, key, placeholders));
    }

    /**
     * Resolves the substituted template string for a locale without parsing it,
     * escaping placeholder values so they cannot inject MiniMessage tags.
     *
     * @param locale       the target locale id
     * @param key          the message key
     * @param placeholders placeholder values to substitute
     * @return the substituted template string
     */
    @Override
    public @NotNull String resolve(@NotNull String locale, @NotNull String key,
                                   @NotNull Placeholder... placeholders) {
        return substitute(template(locale, key), placeholders);
    }

    /**
     * Renders a message for an audience by resolving its locale first.
     *
     * @param audience     the recipient audience
     * @param key          the message key
     * @param placeholders placeholder values to substitute
     * @return the rendered component
     */
    @Override
    public @NotNull Component render(@NotNull Audience audience, @NotNull String key,
                                     @NotNull Placeholder... placeholders) {
        return render(resolver.resolve(audience), key, placeholders);
    }

    /**
     * Renders and immediately sends a message to the given audience.
     *
     * @param audience     the recipient audience
     * @param key          the message key
     * @param placeholders placeholder values to substitute
     */
    @Override
    public void send(@NotNull Audience audience, @NotNull String key, @NotNull Placeholder... placeholders) {
        audience.sendMessage(render(audience, key, placeholders));
    }
}
