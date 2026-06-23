package com.nickax.nexus.api.command;

/**
 * The outcome of dispatching a command.
 */
public enum CommandResult {

    /** Executed successfully. */
    SUCCESS,

    /** The sender lacked the required permission. */
    NO_PERMISSION,

    /** The command requires a player but the console ran it. */
    PLAYER_ONLY,

    /** Arguments were missing or invalid. */
    INVALID_USAGE,

    /** This node has no executor (it is a group of subcommands). */
    NO_EXECUTOR
}
