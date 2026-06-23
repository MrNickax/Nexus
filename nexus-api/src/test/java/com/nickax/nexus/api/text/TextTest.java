package com.nickax.nexus.api.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextTest {

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void of_parsesMiniMessageToComponent() {
        Component component = Text.of("<red>Hello</red>");
        assertEquals("Hello", plain(component));
    }

    @Test
    void of_list_parsesEachLine() {
        List<Component> lines = Text.of(List.of("<gray>one", "<gray>two"));
        assertEquals(2, lines.size());
        assertEquals("one", plain(lines.get(0)));
        assertEquals("two", plain(lines.get(1)));
    }

    @Test
    void of_varargs_parsesEachLine() {
        List<Component> lines = Text.of("a", "b", "c");
        assertEquals(List.of("a", "b", "c"), lines.stream().map(TextTest::plain).toList());
    }
}
