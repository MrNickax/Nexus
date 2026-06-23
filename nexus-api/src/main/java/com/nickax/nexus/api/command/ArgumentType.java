package com.nickax.nexus.api.command;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Parses a command argument token into a typed value and offers tab-completions.
 *
 * @param <T> the parsed type
 */
public interface ArgumentType<T> {

    /**
     * Parses a token.
     *
     * @param input the raw token (already joined for greedy types)
     * @return the parsed value
     * @throws ArgumentParseException if the token is invalid
     */
    @NotNull T parse(@NotNull String input) throws ArgumentParseException;

    /**
     * Suggests completions for a partial token.
     *
     * @param partial the partial token
     * @return suggestions (possibly empty)
     */
    default @NotNull List<String> suggest(@NotNull String partial) {
        return List.of();
    }

    /**
     * @return whether this type consumes all remaining tokens (joined by spaces)
     */
    default boolean greedy() {
        return false;
    }
}
