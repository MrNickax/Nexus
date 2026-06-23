package com.nickax.nexus.core.data;

import com.nickax.nexus.api.data.Backend;
import com.nickax.nexus.api.data.KeyMapper;
import com.nickax.nexus.api.data.WritePolicy;
import com.nickax.nexus.core.lock.LocalLockService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class DataStoreWriteBehindTest {

    static final class FakeBackend<V> implements Backend<V> {
        final Map<String, V> map = new HashMap<>();
        @Override public @NotNull java.util.concurrent.CompletableFuture<Optional<V>> get(@NotNull String k) {
            return java.util.concurrent.CompletableFuture.completedFuture(Optional.ofNullable(map.get(k)));
        }
        @Override public @NotNull java.util.concurrent.CompletableFuture<Void> put(@NotNull String k, @NotNull V v) {
            map.put(k, v); return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
        @Override public @NotNull java.util.concurrent.CompletableFuture<Void> remove(@NotNull String k) {
            map.remove(k); return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
        @Override public @NotNull java.util.concurrent.CompletableFuture<Boolean> contains(@NotNull String k) {
            return java.util.concurrent.CompletableFuture.completedFuture(map.containsKey(k));
        }
        @Override public @NotNull java.util.concurrent.CompletableFuture<Map<String, V>> all() {
            return java.util.concurrent.CompletableFuture.completedFuture(new HashMap<>(map));
        }
    }

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @AfterEach
    void stop() {
        scheduler.shutdownNow();
    }

    private DataStoreImpl<String, String> store(FakeBackend<String> backend, WritePolicy policy) {
        return new DataStoreImpl<>(KeyMapper.string(), new MemoryCache<>(), backend,
                new LocalLockService(), policy, scheduler);
    }

    @Test
    void writeBehind_save_doesNotPersistImmediately_butFindWorks() {
        FakeBackend<String> backend = new FakeBackend<>();
        DataStoreImpl<String, String> store = store(backend, WritePolicy.writeBehind(Duration.ofHours(1)));
        store.save("a", "1").join();
        assertNull(backend.map.get("a"), "write-behind must not persist immediately");
        assertEquals(Optional.of("1"), store.find("a").join(), "find reads from cache");
    }

    @Test
    void writeBehind_flush_persistsDirty() {
        FakeBackend<String> backend = new FakeBackend<>();
        DataStoreImpl<String, String> store = store(backend, WritePolicy.writeBehind(Duration.ofHours(1)));
        store.save("a", "1").join();
        store.flush().join();
        assertEquals("1", backend.map.get("a"));
    }

    @Test
    void writeBehind_delete_tombstones_removedFromBackendOnFlush() {
        FakeBackend<String> backend = new FakeBackend<>();
        backend.map.put("a", "old");
        DataStoreImpl<String, String> store = store(backend, WritePolicy.writeBehind(Duration.ofHours(1)));
        store.delete("a").join();
        assertEquals("old", backend.map.get("a"), "not yet flushed");
        store.flush().join();
        assertFalse(backend.map.containsKey("a"), "tombstone applied on flush");
    }

    @Test
    void writeBehind_periodicFlush_persistsWithoutManualFlush() throws Exception {
        FakeBackend<String> backend = new FakeBackend<>();
        DataStoreImpl<String, String> store = store(backend, WritePolicy.writeBehind(Duration.ofMillis(50)));
        store.save("a", "1").join();
        long deadline = System.currentTimeMillis() + 3000;
        while (backend.map.get("a") == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertEquals("1", backend.map.get("a"), "periodic flush should persist within the interval");
    }

    @Test
    void writeThrough_save_persistsImmediately() {
        FakeBackend<String> backend = new FakeBackend<>();
        DataStoreImpl<String, String> store = store(backend, WritePolicy.writeThrough());
        store.save("a", "1").join();
        assertEquals("1", backend.map.get("a"));
    }

    @Test
    void writeBehind_concurrentSaveAndFlush_persistsFinalValue() throws Exception {
        FakeBackend<String> backend = new FakeBackend<>();
        DataStoreImpl<String, String> store = store(backend, WritePolicy.writeBehind(java.time.Duration.ofHours(1)));
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        int n = 500;
        try {
            for (int i = 0; i < n; i++) {
                String value = "v" + i;
                java.util.concurrent.Future<?> save = pool.submit(() -> store.save("a", value).join());
                java.util.concurrent.Future<?> flush = pool.submit(() -> store.flush().join());
                save.get();
                flush.get();
            }
        } finally {
            pool.shutdownNow();
        }
        store.flush().join();
        assertEquals("v" + (n - 1), backend.map.get("a"), "final saved value must persist after a concurrent save/flush loop");
    }
}
