package com.nickax.nexus.bukkit.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenusImplTest {

    private ServerMock server;
    private Plugin plugin;

    @BeforeEach
    void start() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("Test");
    }

    @AfterEach
    void stop() { MockBukkit.unmock(); }

    @Test
    void open_showsTheMenuInventory() {
        MenusImpl menus = new MenusImpl(plugin);
        Player player = server.addPlayer();
        Menu menu = Menu.chest(1, "x")
                .button(Slot.index(0), Button.of(new ItemStack(Material.DIAMOND)))
                .build();

        menus.open(player, menu);

        assertEquals(9, player.getOpenInventory().getTopInventory().getSize());
        assertEquals(Material.DIAMOND, player.getOpenInventory().getTopInventory().getItem(0).getType());
    }

    @Test
    void click_invokesHandler_andIsCancelled() {
        MenusImpl menus = new MenusImpl(plugin);
        PlayerMock player = server.addPlayer();
        AtomicBoolean clicked = new AtomicBoolean(false);
        Menu menu = Menu.chest(1, "x")
                .button(Slot.index(0), Button.of(new ItemStack(Material.DIAMOND))
                        .onClick(ctx -> clicked.set(true)))
                .build();
        menus.open(player, menu);

        // simulateInventoryClick fires the event through the plugin manager with getRawSlot() == 0
        InventoryClickEvent event = player.simulateInventoryClick(
                player.getOpenInventory(), ClickType.LEFT, 0);

        assertTrue(clicked.get(), "handler should run");
        assertTrue(event.isCancelled(), "click should be cancelled");
    }

    @Test
    void shiftClickInBottomInventory_isCancelled() {
        MenusImpl menus = new MenusImpl(plugin);
        PlayerMock player = server.addPlayer();
        AtomicBoolean clicked = new AtomicBoolean(false);
        Menu menu = Menu.chest(1, "x")
                .button(Slot.index(0), Button.of(new ItemStack(Material.DIAMOND)).onClick(c -> clicked.set(true)))
                .build();
        menus.open(player, menu);
        // a raw slot in the bottom (player) inventory: top is 9 slots, so raw slot 10 is in the bottom
        InventoryClickEvent event = player.simulateInventoryClick(player.getOpenInventory(), ClickType.SHIFT_LEFT, 10);
        assertTrue(event.isCancelled(), "bottom shift-click must be cancelled");
        assertFalse(clicked.get(), "no button handler for a bottom click");
    }

    @Test
    void dragAcrossMenu_isCancelled() {
        MenusImpl menus = new MenusImpl(plugin);
        PlayerMock player = server.addPlayer();
        Menu menu = Menu.chest(1, "x")
                .button(Slot.index(0), Button.of(new ItemStack(Material.DIAMOND)))
                .build();
        menus.open(player, menu);

        // Build an InventoryDragEvent that touches slot 0 in the top inventory (the menu).
        ItemStack cursor = new ItemStack(Material.STONE);
        Map<Integer, ItemStack> newItems = Map.of(0, cursor);
        InventoryDragEvent event = new InventoryDragEvent(
                player.getOpenInventory(),
                null,
                cursor,
                false,
                newItems
        );
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled(), "drag intersecting the menu must be cancelled");
    }
}
