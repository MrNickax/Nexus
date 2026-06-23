package com.nickax.nexus.core.data;

import com.nickax.nexus.api.data.DataStore;
import com.nickax.nexus.api.data.KeyMapper;
import com.nickax.nexus.api.data.WritePolicy;
import com.nickax.nexus.core.NexusImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WriteBehindShutdownTest {

    @Test
    void shutdown_flushesWriteBehindStores(@TempDir Path dir) {
        NexusImpl nexus = new NexusImpl(dir);
        DataStore<String, String> store = nexus.data().<String, String>store("wb", String.class)
                .key(KeyMapper.string())
                .writePolicy(WritePolicy.writeBehind(Duration.ofHours(1))) // long interval: only shutdown flushes
                .build();
        store.save("a", "kept").join();

        nexus.shutdown(); // must flush before stopping executors

        // reopen the same file-backed store via a fresh hub and confirm the value persisted
        NexusImpl reopened = new NexusImpl(dir);
        try {
            DataStore<String, String> store2 = reopened.data().<String, String>store("wb", String.class)
                    .key(KeyMapper.string()).build();
            assertEquals(Optional.of("kept"), store2.find("a").join());
        } finally {
            reopened.shutdown();
        }
    }
}
