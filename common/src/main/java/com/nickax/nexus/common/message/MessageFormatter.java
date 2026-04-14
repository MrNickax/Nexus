package com.nickax.nexus.common.message;

import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * Abstract base class for colorizing text messages.
 * Implementations of this class provide different strategies for converting
 * plain text strings into colored Adventure Components.
 */
public abstract class MessageFormatter {

    /**
     * Converts a list of plain text inputs into a single colorized Adventure Component.
     * Each input string is deserialized individually and then combined into a single component.
     *
     * @param input the list of plain text inputs to colorize
     * @return a Component with applied color formatting from all input strings
     */
    public Component deserialize(List<String> input) {
        return input.stream().map(this::deserialize).reduce(Component.empty(), (a, b) -> a.equals(Component.empty()) ? b : a.append(Component.newline()).append(b));
    }

    /**
     * Converts a plain text input into a colorized Adventure Component.
     *
     * @param input the plain text input to colorize
     * @return a Component with applied color formatting
     */
    public abstract Component deserialize(String input);
}