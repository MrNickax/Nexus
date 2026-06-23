package com.nickax.nexus.bukkit.menu;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MenuTest {

    private ServerMock server;

    @BeforeEach
    void start() { server = MockBukkit.mock(); }

    @AfterEach
    void stop() { MockBukkit.unmock(); }

    @Test
    void chest_buildsWithSizeAndButtons() {
        Menu menu = Menu.chest(3, "<red>Title")
                .button(Slot.of(1, 4), Button.of(new ItemStack(Material.DIAMOND)))
                .build();
        assertEquals(27, menu.size());
        assertNotNull(menu.buttonAt(13)); // row 1 col 4 = 13
        assertNull(menu.buttonAt(0));
    }

    @Test
    void fill_doesNotOverwriteExistingButtons() {
        Button placed = Button.of(new ItemStack(Material.DIAMOND));
        Menu menu = Menu.chest(1, "x")
                .button(Slot.index(0), placed)
                .fill(Button.of(new ItemStack(Material.GRAY_STAINED_GLASS_PANE)))
                .build();
        assertEquals(placed, menu.buttonAt(0));
        assertNotNull(menu.buttonAt(8)); // filled
    }

    @Test
    void chest_invalidRows_throws() {
        assertThrows(IllegalArgumentException.class, () -> Menu.chest(7, "x"));
    }
}
