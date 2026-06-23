package com.nickax.nexus.api.command;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown by an {@link ArgumentType} when an input token cannot be parsed.
 */
public final class ArgumentParseException extends Exception {

    /**
     * @param message a human-readable reason
     */
    public ArgumentParseException(@NotNull String message) {
        super(message);
    }
}
