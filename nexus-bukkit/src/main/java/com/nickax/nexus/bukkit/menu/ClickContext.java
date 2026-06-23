package com.nickax.nexus.bukkit.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

/**
 * Context passed to a button's click handler.
 */
public interface ClickContext {

    /**
     * Returns the player who clicked the menu button.
     *
     * @return the clicking player
     */
    @NotNull Player player();

    /**
     * Returns the type of click that triggered this context (left, right, shift, etc.).
     *
     * @return the click type
     */
    @NotNull ClickType clickType();

    /**
     * Returns the raw slot index that was clicked within the top inventory.
     *
     * @return the clicked raw slot index
     */
    int slot();

    /**
     * Closes the menu for the player (on the next tick).
     */
    void close();
}
