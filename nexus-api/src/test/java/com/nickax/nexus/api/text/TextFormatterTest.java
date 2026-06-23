package com.nickax.nexus.api.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextFormatterTest {

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static String markup(Component component) {
        return MiniMessage.miniMessage().serialize(component).toLowerCase();
    }

    @Test
    void miniMessage_parsesTags() {
        Component component = TextFormatter.miniMessage().deserialize("<red>Hello");
        assertEquals("Hello", plain(component));
        assertTrue(markup(component).contains("red"));
    }

    @Test
    void legacy_parsesAmpersandCodes() {
        Component component = TextFormatter.legacy().deserialize("&cHello");
        assertEquals("Hello", plain(component));
        assertTrue(markup(component).contains("red"));
    }

    @Test
    void legacy_parsesHexColors() {
        Component component = TextFormatter.legacy().deserialize("&#ff0000Hi");
        assertEquals("Hi", plain(component));
        assertTrue(markup(component).contains("#ff0000"));
    }

    @Test
    void mixed_supportsLegacyMiniMessageAndHex() {
        Component component = TextFormatter.mixed().deserialize("&cRed <green>Green</green> &#ff8800Hex");
        assertEquals("Red Green Hex", plain(component));
        String markup = markup(component);
        assertTrue(markup.contains("red"));
        assertTrue(markup.contains("green"));
        assertTrue(markup.contains("#ff8800"));
    }

    @Test
    void of_mapsEachFormatToItsFormatter() {
        assertSame(TextFormatter.miniMessage(), TextFormatter.of(TextFormat.MINI_MESSAGE));
        assertSame(TextFormatter.legacy(), TextFormatter.of(TextFormat.LEGACY));
        assertSame(TextFormatter.mixed(), TextFormatter.of(TextFormat.MIXED));
    }

    @Test
    void deserialize_list_returnsOneComponentPerLine() {
        List<Component> lines = TextFormatter.miniMessage().deserialize(List.of("<red>a", "<blue>b"));
        assertEquals(2, lines.size());
        assertEquals("a", plain(lines.get(0)));
        assertEquals("b", plain(lines.get(1)));
    }
}
