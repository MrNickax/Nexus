package com.nickax.nexus.api.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArgumentsTest {

    enum Color { RED, GREEN, BLUE }

    @Test
    void integer_parsesAndRangeChecks() throws Exception {
        ArgumentType<Integer> type = Arguments.integer(1, 10);
        assertEquals(5, type.parse("5"));
        assertThrows(ArgumentParseException.class, () -> type.parse("0"));
        assertThrows(ArgumentParseException.class, () -> type.parse("x"));
    }

    @Test
    void word_parsesToken() throws Exception {
        assertEquals("hi", Arguments.word().parse("hi"));
    }

    @Test
    void greedyString_isGreedy() {
        assertTrue(Arguments.greedyString().greedy());
    }

    @Test
    void bool_parsesAndSuggests() throws Exception {
        assertEquals(true, Arguments.bool().parse("true"));
        assertEquals(List.of("true", "false"), Arguments.bool().suggest(""));
    }

    @Test
    void enumValue_parsesCaseInsensitively_andSuggests() throws Exception {
        ArgumentType<Color> type = Arguments.enumValue(Color.class);
        assertEquals(Color.RED, type.parse("red"));
        assertThrows(ArgumentParseException.class, () -> type.parse("purple"));
        assertTrue(type.suggest("").contains("GREEN"));
    }
}
