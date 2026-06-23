package com.nickax.nexus.api.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Produces the message shown to a sender when a command does not complete
 * successfully — permission denied, player-only, invalid usage, or an unknown
 * subcommand.
 * <p>
 * Supplied at registration time via {@code commands().register(command, feedback)},
 * this is the seam where a plugin plugs in its own localisation: build a per-locale
 * MiniMessage string for the {@link Sender} and return it, or return {@code null} to
 * stay silent. The returned string is MiniMessage markup, which the platform parses
 * and sends; messages are plain strings (not Adventure components) so the contract is
 * usable from any plugin regardless of how it bundles Adventure. {@link #defaults()}
 * returns plain English fallbacks for plugins that do not care.
 */
public interface CommandFeedback {

    /**
     * Returns the MiniMessage markup to send to {@code sender} for a non-success
     * {@code result}, or {@code null} to send nothing.
     *
     * @param sender the sender that ran the command
     * @param result the non-success dispatch result (never {@link CommandResult#SUCCESS})
     * @return the MiniMessage markup to send, or {@code null} to stay silent
     */
    @Nullable String message(@NotNull Sender sender, @NotNull CommandResult result);

    /**
     * Returns the built-in feedback: short, plain-English messages for each failure
     * result, and {@code null} for {@link CommandResult#SUCCESS}.
     *
     * @return the default feedback
     */
    static @NotNull CommandFeedback defaults() {
        return (sender, result) -> switch (result) {
            case NO_PERMISSION -> "You don't have permission.";
            case PLAYER_ONLY -> "Only players can use this.";
            case INVALID_USAGE -> "Invalid usage.";
            case NO_EXECUTOR -> "Unknown subcommand.";
            case SUCCESS -> null;
        };
    }
}
