package com.nickax.nexus.bukkit.menu;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Opens and closes Nexus menus. Obtain via {@code BukkitNexus.menus()}.
 */
public interface Menus {

    /**
     * Opens a menu for a player.
     *
     * @param player the player
     * @param menu   the menu
     */
    void open(@NotNull Player player, @NotNull Menu menu);

    /**
     * Closes whatever inventory the player has open.
     *
     * @param player the player
     */
    void close(@NotNull Player player);
}
