package com.nickax.nexus.bukkit.command;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitSenderTest {

    private ServerMock server;
    private Plugin plugin;
    private BukkitAudiences audiences;

    @BeforeEach
    void start() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("Test");
        audiences = BukkitAudiences.create(plugin);
    }

    @AfterEach
    void stop() {
        audiences.close();
        MockBukkit.unmock();
    }

    @Test
    void player_exposesNameUuidAndIsPlayer() {
        Player player = server.addPlayer("Steve");
        BukkitSender sender = new BukkitSender(player, audiences);
        assertEquals("Steve", sender.name());
        assertTrue(sender.isPlayer());
        assertTrue(sender.uuid().isPresent());
        assertEquals(player.getUniqueId(), sender.uuid().get());
    }

    @Test
    void console_isNotPlayer_noUuid() {
        BukkitSender sender = new BukkitSender(server.getConsoleSender(), audiences);
        assertFalse(sender.isPlayer());
        assertFalse(sender.uuid().isPresent());
    }
}
