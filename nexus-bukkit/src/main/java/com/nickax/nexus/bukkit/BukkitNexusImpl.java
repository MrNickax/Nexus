package com.nickax.nexus.bukkit;

import com.nickax.nexus.bukkit.command.BukkitCommands;
import com.nickax.nexus.bukkit.command.Commands;
import com.nickax.nexus.bukkit.listener.Listeners;
import com.nickax.nexus.bukkit.listener.ListenersImpl;
import com.nickax.nexus.bukkit.menu.Menus;
import com.nickax.nexus.bukkit.menu.MenusImpl;
import com.nickax.nexus.bukkit.schedule.BukkitScheduler;
import com.nickax.nexus.bukkit.schedule.SchedulerFactory;
import com.nickax.nexus.bukkit.text.Messages;
import com.nickax.nexus.bukkit.text.MessagesImpl;
import com.nickax.nexus.core.NexusImpl;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Server-side {@link com.nickax.nexus.api.Nexus} implementation. Installs the
 * Bukkit/Folia scheduler and owns the listener registry.
 */
public final class BukkitNexusImpl extends NexusImpl implements BukkitNexus {

    private final BukkitScheduler bukkitScheduler;
    private final Listeners listeners;
    private final BukkitAudiences audiences;
    private final Commands commands;
    private final Menus menus;
    private final Messages messages;

    /**
     * Creates the server-side hub.
     *
     * @param dataFolder the data folder for file-backed stores and the persisted node id
     * @param plugin     the Nexus plugin instance
     */
    public BukkitNexusImpl(@NotNull Path dataFolder, @NotNull Plugin plugin) {
        super(dataFolder);
        this.bukkitScheduler = SchedulerFactory.create(plugin);
        installScheduler(bukkitScheduler);
        this.listeners = new ListenersImpl();
        this.audiences = BukkitAudiences.create(plugin);
        try {
            this.commands = new BukkitCommands(plugin, audiences);
            this.menus = new MenusImpl(plugin);
            this.messages = new MessagesImpl(audiences);
        } catch (RuntimeException e) {
            audiences.close();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull BukkitScheduler scheduler() {
        return bukkitScheduler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Listeners listeners() {
        return listeners;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Menus menus() {
        return menus;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Messages messages() {
        return messages;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Audience audience(@NotNull CommandSender sender) {
        return audiences.sender(sender);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Commands commands() {
        return commands;
    }

    /**
     * Unregisters all listeners and commands, closes the Adventure audiences
     * provider, then delegates to the parent shutdown.
     */
    @Override
    public void shutdown() {
        listeners.unregisterAll();
        commands.unregisterAll();
        audiences.close();
        super.shutdown();
    }
}
