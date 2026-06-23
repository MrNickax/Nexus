package com.nickax.nexus.core;

import com.nickax.nexus.api.NexusScope;
import com.nickax.nexus.api.data.DataStore;
import com.nickax.nexus.api.data.KeyMapper;
import com.nickax.nexus.api.service.Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NexusScopeImplTest {

    private NexusImpl nexus(Path dir) {
        return new NexusImpl(dir);
    }

    @Test
    void scopedStores_areNamespaced_andDoNotCollide(@TempDir Path dir) {
        NexusImpl nexus = nexus(dir);
        try {
            DataStore<String, String> a = nexus.scope("alpha")
                    .data().<String, String>store("data", String.class).key(KeyMapper.string()).build();
            DataStore<String, String> b = nexus.scope("beta")
                    .data().<String, String>store("data", String.class).key(KeyMapper.string()).build();

            a.save("k", "fromAlpha").join();
            b.save("k", "fromBeta").join();

            assertEquals(Optional.of("fromAlpha"), a.find("k").join());
            assertEquals(Optional.of("fromBeta"), b.find("k").join());
            // distinct backing directories under data/
            assertTrue(Files.isDirectory(dir.resolve("data").resolve("alpha_data")));
            assertTrue(Files.isDirectory(dir.resolve("data").resolve("beta_data")));
        } finally {
            nexus.shutdown();
        }
    }

    @Test
    void close_stopsScopedServices(@TempDir Path dir) {
        NexusImpl nexus = nexus(dir);
        try {
            NexusScope scope = nexus.scope("alpha");
            AtomicBoolean stopped = new AtomicBoolean(false);
            scope.register(new Service() {
                @Override public void onStart() { }
                @Override public void onStop() { stopped.set(true); }
            });
            scope.close();
            assertTrue(stopped.get());
        } finally {
            nexus.shutdown();
        }
    }

    @Test
    void scope_invalidId_throws(@TempDir Path dir) {
        NexusImpl nexus = nexus(dir);
        try {
            assertThrows(IllegalArgumentException.class, () -> nexus.scope("bad id!"));
        } finally {
            nexus.shutdown();
        }
    }

    @Test
    void close_isIdempotent(@TempDir java.nio.file.Path dir) {
        NexusImpl nexus = nexus(dir);
        try {
            com.nickax.nexus.api.NexusScope scope = nexus.scope("alpha");
            scope.close();
            scope.close(); // must not throw
        } finally {
            nexus.shutdown();
        }
    }

    @Test
    void scope_sameId_returnsSameInstance(@TempDir java.nio.file.Path dir) {
        NexusImpl nexus = nexus(dir);
        try {
            org.junit.jupiter.api.Assertions.assertSame(nexus.scope("alpha"), nexus.scope("alpha"));
        } finally {
            nexus.shutdown();
        }
    }
}
