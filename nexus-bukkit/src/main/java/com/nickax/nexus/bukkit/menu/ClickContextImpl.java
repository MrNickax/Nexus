package com.nickax.nexus.bukkit.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

/**
 * Default {@link ClickContext}.
 */
final class ClickContextImpl implements ClickContext {

    private final org.bukkit.plugin.Plugin plugin;
    private final Player player;
    private final ClickType clickType;
    private final int slot;

    /**
     * Constructs a click context from a raw inventory click event.
     *
     * @param plugin    the owning plugin, used for scheduling the close task
     * @param player    the player who clicked
     * @param clickType the type of click (left, right, shift, etc.)
     * @param slot      the raw slot index that was clicked
     */
    ClickContextImpl(@NotNull org.bukkit.plugin.Plugin plugin, @NotNull Player player, @NotNull ClickType clickType, int slot) {
        this.plugin = plugin;
        this.player = player;
        this.clickType = clickType;
        this.slot = slot;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Player player() {
        return player;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull ClickType clickType() {
        return clickType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int slot() {
        return slot;
    }

    /**
     * Schedules an inventory close for the player on the next server tick to avoid
     * closing the inventory from within an active inventory event handler.
     */
    @Override
    public void close() {
        plugin.getServer().getScheduler().runTask(plugin, (Runnable) player::closeInventory);
    }
}
