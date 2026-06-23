package com.nickax.nexus.bukkit;

import com.nickax.nexus.api.Nexus;
import com.nickax.nexus.api.NexusProvider;
import com.nickax.nexus.api.NexusScope;
import com.nickax.nexus.bukkit.command.Commands;
import com.nickax.nexus.bukkit.listener.Listeners;
import com.nickax.nexus.bukkit.menu.Menus;
import com.nickax.nexus.bukkit.schedule.BukkitScheduler;
import com.nickax.nexus.bukkit.text.Messages;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Server-side Nexus hub. Extends the agnostic {@link Nexus} with accessors that
 * only exist on a Bukkit server (menus and items are added in a later plan).
 */
public interface BukkitNexus extends Nexus {

    /**
     * The server-side scheduler with region- and entity-bound scheduling.
     *
     * @return the Bukkit scheduler
     */
    @Override
    @NotNull BukkitScheduler scheduler();

    /**
     * The managed Bukkit listener registry.
     *
     * @return the listeners registry
     */
    @NotNull Listeners listeners();

    /**
     * The menu (GUI) manager.
     *
     * @return the menus manager
     */
    @NotNull Menus menus();

    /**
     * The string-based messaging service: formats markup with a chosen
     * {@link com.nickax.nexus.api.text.TextFormat} and sends it, without exposing
     * Adventure types to the caller.
     *
     * @return the messages service
     */
    @NotNull Messages messages();

    /**
     * Adapts a Bukkit sender to an Adventure audience (Spigot/Paper safe). Use with
     * {@code nexus.lang().send(audience, ...)}.
     *
     * @param sender the Bukkit sender
     * @return the audience
     */
    @NotNull Audience audience(@NotNull CommandSender sender);

    /**
     * The command registry for registering Nexus commands.
     *
     * @return the commands registry
     */
    @NotNull Commands commands();

    /**
     * Convenience accessor for the server-side hub.
     *
     * @return the active hub cast to {@link BukkitNexus}
     * @throws IllegalStateException if Nexus is not loaded
     */
    static @NotNull BukkitNexus get() {
        Nexus nexus = NexusProvider.get();
        if (!(nexus instanceof BukkitNexus bukkit)) {
            throw new IllegalStateException("Active Nexus instance is not a BukkitNexus (got "
                    + nexus.getClass().getSimpleName() + ")");
        }
        return bukkit;
    }

    /**
     * Returns a scope for a plugin, deriving the id from the plugin name (lowercased,
     * with any non-{@code [a-z0-9_]} character replaced by {@code _}).
     * Plugin names that differ only in characters outside {@code [a-z0-9_]} (e.g. {@code "My-Plugin"} vs {@code "My_Plugin"}) sanitize to the same id; this is harmless on a normal server where loaded plugin names are unique.
     *
     * @param plugin the owning plugin
     * @return a scope bound to the plugin
     */
    default @NotNull NexusScope scope(@NotNull Plugin plugin) {
        String id = plugin.getName().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        return scope(id);
    }
}
