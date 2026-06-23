package com.nickax.nexus.core.lang;

import com.nickax.nexus.api.lang.Lang;
import com.nickax.nexus.api.lang.Placeholder;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StandardLangTest {

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** Captures the last component sent. */
    private static final class CapturingAudience implements Audience {
        Component last;
        @Override public void sendMessage(@org.jetbrains.annotations.NotNull Component message) { this.last = message; }
    }

    private Lang lang() {
        return new LangBuilderImpl()
                .defaultLocale("en")
                .resolver(audience -> "es")
                .bundle("en", Map.of("hi", "<red>Hello {player}"))
                .bundle("es", Map.of("hi", "<red>Hola {player}"))
                .build();
    }

    @Test
    void render_explicitLocale_substitutesPlaceholders() {
        assertEquals("Hello Steve", plain(lang().render("en", "hi", Placeholder.of("player", "Steve"))));
        assertEquals("Hola Steve", plain(lang().render("es", "hi", Placeholder.of("player", "Steve"))));
    }

    @Test
    void render_unknownKey_returnsRawKey() {
        assertEquals("missing.key", plain(lang().render("en", "missing.key")));
    }

    @Test
    void render_unknownLocale_fallsBackToDefault() {
        assertEquals("Hello Bob", plain(lang().render("fr", "hi", Placeholder.of("player", "Bob"))));
    }

    @Test
    void render_audience_usesResolvedLocale() {
        // resolver returns "es" for any audience
        assertEquals("Hola Ann", plain(lang().render(new CapturingAudience(), "hi", Placeholder.of("player", "Ann"))));
    }

    @Test
    void send_rendersAndDelivers() {
        CapturingAudience audience = new CapturingAudience();
        lang().send(audience, "hi", Placeholder.of("player", "Zoe"));
        assertEquals("Hola Zoe", plain(audience.last));
    }

    @Test
    void resolve_returnsSubstitutedMarkupWithoutParsing() {
        assertEquals("<red>Hello Steve", lang().resolve("en", "hi", Placeholder.of("player", "Steve")));
    }

    @Test
    void resolve_escapesPlaceholderValues() {
        // A value containing MiniMessage markup must be escaped so it cannot inject a tag.
        assertEquals("<red>Hello \\<red>Hacker", lang().resolve("en", "hi", Placeholder.of("player", "<red>Hacker")));
    }

    @Test
    void render_placeholderValue_isLiteralNotParsed() {
        // A value containing MiniMessage markup must NOT be parsed as a tag (no injection).
        Component component = lang().render("en", "hi", Placeholder.of("player", "<red>Hacker"));
        assertEquals("Hello <red>Hacker", plain(component));
    }
}
