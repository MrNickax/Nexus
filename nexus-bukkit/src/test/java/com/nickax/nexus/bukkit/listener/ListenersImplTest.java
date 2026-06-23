package com.nickax.nexus.bukkit.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListenersImplTest {

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

    static final class CountingListener implements Listener {
        int count;

        @EventHandler
        public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
            count++;
        }
    }

    @Test
    void register_thenEvent_isHandled_andUnregisterAllStopsIt() {
        ListenersImpl listeners = new ListenersImpl();
        CountingListener listener = new CountingListener();
        listeners.register(plugin, listener);

        server.addPlayer(); // fires PlayerJoinEvent
        assertEquals(1, listener.count);

        listeners.unregisterAll();
        server.addPlayer();
        assertEquals(1, listener.count, "no more events after unregisterAll");
    }

    @Test
    void unregister_byOwner_stopsEvents() {
        ListenersImpl listeners = new ListenersImpl();
        CountingListener listener = new CountingListener();
        listeners.register(plugin, listener);

        server.addPlayer();
        assertEquals(1, listener.count);

        listeners.unregister(plugin);
        server.addPlayer();
        assertEquals(1, listener.count, "no more events after unregister(owner)");
    }
}
