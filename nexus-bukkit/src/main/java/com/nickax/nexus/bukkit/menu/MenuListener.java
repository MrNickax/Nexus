package com.nickax.nexus.bukkit.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Cancels clicks and drags in Nexus menus and routes them to the clicked button's handler.
 */
public final class MenuListener implements Listener {

    private final Plugin plugin;

    /**
     * Constructs the menu listener.
     *
     * @param plugin the owning plugin, used to schedule inventory-close tasks
     */
    public MenuListener(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Cancels all clicks inside a Nexus menu and delegates to the clicked button's
     * handler. Clicks outside the menu's slot range (e.g. the player hotbar area) are
     * silently ignored after cancellation.
     *
     * @param event the inventory click event
     */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof MenuHolder menuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= menuHolder.menu().size()) {
            return;
        }
        Button button = menuHolder.menu().buttonAt(event.getRawSlot());
        if (button == null || button.handler() == null) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        button.handler().accept(new ClickContextImpl(plugin, player, event.getClick(), event.getRawSlot()));
    }

    /**
     * Cancels all drag operations that involve a Nexus menu, preventing players from
     * spreading items across menu slots.
     *
     * @param event the inventory drag event
     */
    @EventHandler
    public void onDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }
}
