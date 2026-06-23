package com.nickax.nexus.api.command;

import org.jetbrains.annotations.NotNull;

/**
 * A named positional argument of a command.
 *
 * @param name     the argument name (used for context lookup)
 * @param type     the argument type
 * @param optional whether the argument may be omitted
 */
public record Argument(@NotNull String name, @NotNull ArgumentType<?> type, boolean optional) {
}
