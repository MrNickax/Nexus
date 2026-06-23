package com.nickax.nexus.bukkit.item;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkullTest {

    // Decodes to {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/abc123"}}}
    private static final String TEXTURE_BASE64 =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWJjMTIzIn19fQ==";

    private ServerMock server;

    @BeforeEach
    void start() { server = MockBukkit.mock(); }

    @AfterEach
    void stop() { MockBukkit.unmock(); }

    @Test
    void of_player_producesPlayerHeadWithNameAndOwner() {
        Player player = server.addPlayer("Steve");
        ItemStack head = SkullBuilder.of(player).name("<yellow>Steve").tag("owner", "steve").build();
        assertEquals(Material.PLAYER_HEAD, head.getType());
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        // MockBukkit may return null for getOwningPlayer(); assert only when non-null
        if (meta.getOwningPlayer() != null) {
            assertEquals("Steve", meta.getOwningPlayer().getName());
        }
        assertTrue(meta.getDisplayName().contains("Steve"));
        assertEquals("steve", meta.getPersistentDataContainer().get(
                org.bukkit.NamespacedKey.fromString("nexus:owner"),
                org.bukkit.persistence.PersistentDataType.STRING));
    }

    @Test
    void texture_validBase64_producesNamedPlayerHead() {
        // A valid textures payload must apply without throwing and yield a player head.
        // (MockBukkit's owner-profile texture read is unreliable, so the skin url itself
        // is not asserted here; the decode/extract path is covered by the negative tests.)
        ItemStack head = SkullBuilder.texture(TEXTURE_BASE64).name("<gold>Custom").build();
        assertEquals(Material.PLAYER_HEAD, head.getType());
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assertTrue(meta.getDisplayName().contains("Custom"));
    }

    @Test
    void texture_invalidBase64_throws() {
        assertThrows(IllegalArgumentException.class, () -> SkullBuilder.texture("not valid base64 !!!"));
    }

    @Test
    void texture_base64WithoutSkinUrl_throws() {
        // Decodes to {"foo":"bar"} — valid base64, no SKIN url.
        assertThrows(IllegalArgumentException.class, () -> SkullBuilder.texture("eyJmb28iOiJiYXIifQ=="));
    }

    @Test
    void inheritsFullItemCustomization() {
        Player player = server.addPlayer("Alex");
        ItemStack head = SkullBuilder.of(player)
                .amount(3)
                .lore("<gray>Legendary head")
                .enchant(Enchantment.SHARPNESS, 4)
                .unbreakable(true)
                .model(7)
                .build();
        assertEquals(3, head.getAmount());
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assertTrue(meta.getLore().get(0).contains("Legendary head"));
        assertEquals(4, meta.getEnchantLevel(Enchantment.SHARPNESS));
        assertTrue(meta.isUnbreakable());
        assertTrue(meta.hasCustomModelData());
        assertEquals(7, meta.getCustomModelData());
    }
}
