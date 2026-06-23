package com.nickax.nexus.bukkit.menu;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link InventoryHolder} that tags a Nexus menu inventory and carries its
 * {@link Menu}, so the click listener can recognise Nexus menus.
 */
public final class MenuHolder implements InventoryHolder {

    private final Menu menu;
    private final Inventory inventory;

    /**
     * Creates the Bukkit inventory for the given menu, populating every slot with
     * a clone of its button's icon.
     *
     * @param menu the menu to back this holder
     */
    MenuHolder(@NotNull Menu menu) {
        this.menu = menu;
        this.inventory = Bukkit.createInventory(this, menu.size(), menu.title());
        menu.buttons().forEach((slot, button) -> inventory.setItem(slot, button.icon().clone()));
    }

    /**
     * Returns the menu this holder was created for.
     *
     * @return the menu
     */
    @NotNull Menu menu() {
        return menu;
    }

    /**
     * Returns the Bukkit inventory that represents this menu.
     *
     * @return the inventory
     */
    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
