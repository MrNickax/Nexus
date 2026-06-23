package com.nickax.nexus.bukkit.item;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemTest {

    private ServerMock server;

    @BeforeEach
    void start() { server = MockBukkit.mock(); }

    @AfterEach
    void stop() { MockBukkit.unmock(); }

    @Test
    void build_setsMaterialAmountNameLore() {
        ItemStack stack = ItemBuilder.of(Material.DIAMOND_SWORD)
                .amount(2)
                .name("<red>Excalibur")
                .lore("<gray>Legendary")
                .build();
        assertEquals(Material.DIAMOND_SWORD, stack.getType());
        assertEquals(2, stack.getAmount());
        ItemMeta meta = stack.getItemMeta();
        // legacy-section serialized; the plain text must be present
        assertTrue(meta.getDisplayName().contains("Excalibur"));
        assertTrue(meta.getLore().get(0).contains("Legendary"));
    }

    @Test
    void build_appliesEnchantsFlagsUnbreakableModel() {
        ItemStack stack = ItemBuilder.of(Material.DIAMOND_SWORD)
                .enchant(Enchantment.SHARPNESS, 5)
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .unbreakable(true)
                .model(1001)
                .build();
        ItemMeta meta = stack.getItemMeta();
        assertEquals(5, meta.getEnchantLevel(Enchantment.SHARPNESS));
        assertTrue(meta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES));
        assertTrue(meta.isUnbreakable());
        assertTrue(meta.hasCustomModelData());
        assertEquals(1001, meta.getCustomModelData());
    }

    @Test
    void tag_writesStringPdc() {
        ItemStack stack = ItemBuilder.of(Material.PAPER).tag("token", "abc").build();
        ItemMeta meta = stack.getItemMeta();
        String value = meta.getPersistentDataContainer().get(
                org.bukkit.NamespacedKey.fromString("nexus:token"), PersistentDataType.STRING);
        assertEquals("abc", value);
    }
}
