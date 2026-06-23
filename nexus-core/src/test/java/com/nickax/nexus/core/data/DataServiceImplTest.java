package com.nickax.nexus.core.data;

import com.nickax.nexus.api.data.DataStore;
import com.nickax.nexus.api.data.KeyMapper;
import com.nickax.nexus.core.lock.LocalLockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataServiceImplTest {

    private static final Executor DIRECT = Runnable::run;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @AfterEach
    void stop() {
        scheduler.shutdownNow();
    }

    @Test
    void build_andRoundTripThroughFileBackend(@TempDir Path dir) {
        DataServiceImpl service = new DataServiceImpl(dir, DIRECT, new LocalLockService(),
                new ConnectionRegistry(), scheduler);
        DataStore<UUID, String> store = service.<UUID, String>store("players", String.class)
                .key(KeyMapper.uuid())
                .build();

        UUID id = UUID.randomUUID();
        store.save(id, "Steve").join();
        assertEquals(Optional.of("Steve"), store.find(id).join());

        // a fresh store over the same folder sees the persisted value
        DataStore<UUID, String> reopened = new DataServiceImpl(dir, DIRECT, new LocalLockService(),
                new ConnectionRegistry(), scheduler)
                .<UUID, String>store("players", String.class).key(KeyMapper.uuid()).build();
        assertEquals(Optional.of("Steve"), reopened.find(id).join());
    }

    @Test
    void build_withoutKeyMapper_throws(@TempDir Path dir) {
        DataServiceImpl service = new DataServiceImpl(dir, DIRECT, new LocalLockService(),
                new ConnectionRegistry(), scheduler);
        assertThrows(IllegalStateException.class,
                () -> service.store("x", String.class).build());
    }
}
