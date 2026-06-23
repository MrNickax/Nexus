package com.nickax.nexus.bukkit.schedule;

import com.nickax.nexus.bukkit.schedule.BukkitScheduler;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SchedulerFactoryTest {

    private ServerMock server;
    private Plugin plugin;

    @BeforeEach
    void start() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("Test");
    }

    @AfterEach
    void stop() {
        MockBukkit.unmock();
    }

    @Test
    void create_returnsBukkitSchedulerWhenNotFolia() {
        BukkitScheduler scheduler = SchedulerFactory.create(plugin);
        assertInstanceOf(BukkitSchedulerImpl.class, scheduler);
    }
}
