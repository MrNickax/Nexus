package com.nickax.nexus.api.command;

import org.jetbrains.annotations.NotNull;

/**
 * Runs a command's logic.
 */
@FunctionalInterface
public interface CommandExecutor {

    /**
     * Executes the command.
     *
     * @param context the command context
     */
    void execute(@NotNull CommandContext context);
}
