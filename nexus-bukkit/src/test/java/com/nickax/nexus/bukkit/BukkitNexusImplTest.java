package com.nickax.nexus.bukkit;

import com.nickax.nexus.api.NexusProvider;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitNexusImplTest {

    @TempDir
    Path dir;

    private Plugin plugin;

    @BeforeEach
    void start() {
        MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("Test");
    }

    @AfterEach
    void stop() {
        NexusProvider.set(null);
        MockBukkit.unmock();
    }

    @Test
    void bukkitNexus_isPublishableAndRetrievable() {
        BukkitNexusImpl nexus = new BukkitNexusImpl(dir, plugin);
        NexusProvider.set(nexus);

        assertTrue(NexusProvider.isLoaded());
        assertSame(nexus, BukkitNexus.get());
        assertSame(nexus.services(), BukkitNexus.get().services());
    }

    @Test
    void clear_unloadsProvider() {
        NexusProvider.set(new BukkitNexusImpl(dir, plugin));
        NexusProvider.set(null);
        assertFalse(NexusProvider.isLoaded());
    }
}
