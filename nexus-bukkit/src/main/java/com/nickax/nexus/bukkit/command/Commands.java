package com.nickax.nexus.bukkit.command;

import com.nickax.nexus.api.command.Command;
import com.nickax.nexus.api.command.CommandFeedback;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Registers Nexus commands into the server on behalf of a plugin and unregisters them by
 * owner, so a plugin can clean up exactly its own commands on disable (PlugMan-style
 * unload/reload safe) without touching any other plugin's. Obtain via
 * {@code BukkitNexus.commands()}.
 */
public interface Commands {

    /**
     * Registers a command (and its aliases) owned by {@code owner}, using the
     * {@linkplain CommandFeedback#defaults() default} failure messages.
     *
     * @param owner   the plugin that owns the command
     * @param command the command
     */
    void register(@NotNull Plugin owner, @NotNull Command command);

    /**
     * Registers a command (and its aliases) owned by {@code owner} with custom failure
     * feedback, letting the plugin localise the permission/usage messages it sends.
     *
     * @param owner    the plugin that owns the command
     * @param command  the command
     * @param feedback the feedback used to build messages for non-success results
     */
    void register(@NotNull Plugin owner, @NotNull Command command, @NotNull CommandFeedback feedback);

    /**
     * Unregisters every command registered through this registry for {@code owner},
     * leaving other plugins' commands untouched.
     *
     * @param owner the plugin whose commands should be unregistered
     */
    void unregister(@NotNull Plugin owner);

    /**
     * Unregisters every command registered through this registry, for all owners (called
     * on the Nexus plugin's disable so a reload does not leave stale commands behind).
     */
    void unregisterAll();
}
