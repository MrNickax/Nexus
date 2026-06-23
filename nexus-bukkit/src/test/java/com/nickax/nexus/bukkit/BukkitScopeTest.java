package com.nickax.nexus.bukkit;

import com.nickax.nexus.api.NexusScope;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BukkitScopeTest {

    @TempDir
    Path dir;

    private ServerMock server;

    @BeforeEach
    void start() { server = MockBukkit.mock(); }

    @AfterEach
    void stop() { MockBukkit.unmock(); }

    @Test
    void scope_derivesIdFromPluginName() {
        // build a hub without infra; dataFolder only
        Plugin plugin = MockBukkit.createMockPlugin("MyPlugin");
        BukkitNexusImpl nexus = new BukkitNexusImpl(dir, plugin);
        try {
            NexusScope scope = nexus.scope(plugin);
            assertEquals("myplugin", scope.id());
        } finally {
            nexus.shutdown();
        }
    }
}
