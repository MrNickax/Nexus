package com.nickax.nexus.bukkit.menu;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Default {@link Menus}: opens menus backed by a {@link MenuHolder} and registers
 * the shared {@link MenuListener} once.
 */
public final class MenusImpl implements Menus {

    /**
     * Creates the menu manager and registers its click listener.
     *
     * @param plugin the owning plugin
     */
    public MenusImpl(@NotNull Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new MenuListener(plugin), plugin);
    }

    /**
     * Creates a {@link MenuHolder} for the menu, then opens its inventory for the player.
     *
     * @param player the player to open the menu for
     * @param menu   the menu to open
     */
    @Override
    public void open(@NotNull Player player, @NotNull Menu menu) {
        MenuHolder holder = new MenuHolder(menu);
        player.openInventory(holder.getInventory());
    }

    /**
     * Closes whatever inventory the player currently has open.
     *
     * @param player the player whose inventory view to close
     */
    @Override
    public void close(@NotNull Player player) {
        player.closeInventory();
    }
}
