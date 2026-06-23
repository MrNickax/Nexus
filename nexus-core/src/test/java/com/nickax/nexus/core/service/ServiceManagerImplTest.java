package com.nickax.nexus.core.service;

import com.nickax.nexus.api.service.Service;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceManagerImplTest {

    /** Records start/stop calls into a shared log so order can be asserted. */
    private static final class RecordingService implements Service {
        private final String id;
        private final List<String> log;
        RecordingService(String id, List<String> log) { this.id = id; this.log = log; }
        @Override public void onStart() { log.add("start:" + id); }
        @Override public void onStop() { log.add("stop:" + id); }
        @Override public String name() { return id; }
    }

    @Test
    void register_startsServiceImmediately() {
        List<String> log = new ArrayList<>();
        ServiceManagerImpl manager = new ServiceManagerImpl();

        manager.register("plugin", new RecordingService("a", log));

        assertEquals(List.of("start:a"), log);
    }

    @Test
    void stopAll_stopsInReverseRegistrationOrder() {
        List<String> log = new ArrayList<>();
        ServiceManagerImpl manager = new ServiceManagerImpl();
        manager.register("plugin", new RecordingService("a", log));
        manager.register("plugin", new RecordingService("b", log));
        manager.register("plugin", new RecordingService("c", log));
        log.clear();

        manager.stopAll("plugin");

        assertEquals(List.of("stop:c", "stop:b", "stop:a"), log);
    }

    @Test
    void stopAll_isScopedToOwner() {
        List<String> log = new ArrayList<>();
        ServiceManagerImpl manager = new ServiceManagerImpl();
        manager.register("one", new RecordingService("x", log));
        manager.register("two", new RecordingService("y", log));
        log.clear();

        manager.stopAll("one");

        assertEquals(List.of("stop:x"), log);
    }

    @Test
    void stopAll_continuesWhenAServiceThrows() {
        List<String> log = new ArrayList<>();
        ServiceManagerImpl manager = new ServiceManagerImpl();
        manager.register("plugin", new RecordingService("a", log));
        manager.register("plugin", new Service() {
            @Override public void onStart() { }
            @Override public void onStop() { throw new RuntimeException("boom"); }
        });
        log.clear();

        manager.stopAll("plugin");

        assertTrue(log.contains("stop:a"), "earlier-registered service must still be stopped");
    }
}
