package com.nickax.nexus.api.command;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Per-invocation context: the sender, raw tokens, and parsed typed arguments.
 */
public interface CommandContext {

    /**
     * Returns the sender who executed this command.
     *
     * @return the sender
     */
    @NotNull Sender sender();

    /**
     * Returns the raw argument tokens passed to the executed node.
     *
     * @return the raw argument tokens
     */
    @NotNull String[] raw();

    /**
     * Gets a parsed argument by name.
     *
     * @param name the argument name
     * @param <T>  the expected type
     * @return the parsed value
     * @throws IllegalArgumentException if no such argument was parsed
     */
    <T> @NotNull T get(@NotNull String name);

    /**
     * Returns whether the named argument was provided and successfully parsed.
     *
     * @param name the argument name
     * @return whether the argument was provided and parsed
     */
    boolean has(@NotNull String name);

    /**
     * Returns the parsed int value of the named argument.
     *
     * @param name the argument name
     * @return the parsed int argument
     * @throws IllegalArgumentException if no such argument was parsed
     */
    int getInt(@NotNull String name);

    /**
     * Returns the parsed string value of the named argument.
     *
     * @param name the argument name
     * @return the parsed string argument
     * @throws IllegalArgumentException if no such argument was parsed
     */
    @NotNull String getString(@NotNull String name);

    /**
     * Sends a component to the sender.
     *
     * @param message the message
     */
    void reply(@NotNull Component message);
}
