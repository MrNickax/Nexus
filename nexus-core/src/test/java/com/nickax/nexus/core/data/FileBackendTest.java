package com.nickax.nexus.core.data;

import com.nickax.nexus.api.data.Codec;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileBackendTest {

    private static final Codec<String> CODEC = new Codec<>() {
        @Override public String encode(String value) { return value; }
        @Override public String decode(String data) { return data; }
    };
    private static final Executor DIRECT = Runnable::run;

    @Test
    void put_thenGet_roundTrips(@TempDir Path dir) {
        FileBackend<String> backend = new FileBackend<>(dir, CODEC, DIRECT);
        backend.put("a", "hello").join();
        assertEquals(Optional.of("hello"), backend.get("a").join());
    }

    @Test
    void get_absent_isEmpty(@TempDir Path dir) {
        FileBackend<String> backend = new FileBackend<>(dir, CODEC, DIRECT);
        assertEquals(Optional.empty(), backend.get("missing").join());
    }

    @Test
    void put_persistsAcrossInstances(@TempDir Path dir) {
        new FileBackend<>(dir, CODEC, DIRECT).put("a", "v").join();
        FileBackend<String> reopened = new FileBackend<>(dir, CODEC, DIRECT);
        assertEquals(Optional.of("v"), reopened.get("a").join());
    }

    @Test
    void remove_deletesFile(@TempDir Path dir) {
        FileBackend<String> backend = new FileBackend<>(dir, CODEC, DIRECT);
        backend.put("a", "v").join();
        backend.remove("a").join();
        assertFalse(backend.contains("a").join());
    }

    @Test
    void all_readsEveryEntry(@TempDir Path dir) {
        FileBackend<String> backend = new FileBackend<>(dir, CODEC, DIRECT);
        backend.put("a", "1").join();
        backend.put("b", "2").join();
        Map<String, String> all = backend.all().join();
        assertEquals(Map.of("a", "1", "b", "2"), all);
    }

    @Test
    void all_emptyDir_isEmptyMap(@TempDir Path dir) {
        assertTrue(new FileBackend<>(dir, CODEC, DIRECT).all().join().isEmpty());
    }
}
