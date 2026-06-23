package com.nickax.nexus.core.data;

import com.nickax.nexus.api.data.Backend;
import com.nickax.nexus.api.data.KeyMapper;
import com.nickax.nexus.core.lock.LocalLockService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataStoreImplTest {

    /** Minimal in-memory backend for tests. */
    static final class FakeBackend<V> implements Backend<V> {
        final Map<String, V> map = new HashMap<>();
        @Override public @NotNull CompletableFuture<Optional<V>> get(@NotNull String k) {
            return CompletableFuture.completedFuture(Optional.ofNullable(map.get(k)));
        }
        @Override public @NotNull CompletableFuture<Void> put(@NotNull String k, @NotNull V v) {
            map.put(k, v); return CompletableFuture.completedFuture(null);
        }
        @Override public @NotNull CompletableFuture<Void> remove(@NotNull String k) {
            map.remove(k); return CompletableFuture.completedFuture(null);
        }
        @Override public @NotNull CompletableFuture<Boolean> contains(@NotNull String k) {
            return CompletableFuture.completedFuture(map.containsKey(k));
        }
        @Override public @NotNull CompletableFuture<Map<String, V>> all() {
            return CompletableFuture.completedFuture(new HashMap<>(map));
        }
    }

    private DataStoreImpl<String, String> newStore(FakeBackend<String> backend) {
        return new DataStoreImpl<>(
                KeyMapper.string(), new MemoryCache<>(), backend, new LocalLockService());
    }

    @Test
    void save_thenFind_returnsValue() {
        DataStoreImpl<String, String> store = newStore(new FakeBackend<>());
        store.save("a", "1").join();
        assertEquals(Optional.of("1"), store.find("a").join());
    }

    @Test
    void save_writesThroughToBackend() {
        FakeBackend<String> backend = new FakeBackend<>();
        newStore(backend).save("a", "1").join();
        assertEquals("1", backend.map.get("a"));
    }

    @Test
    void find_fallsBackToBackendAndPopulatesCache() {
        FakeBackend<String> backend = new FakeBackend<>();
        backend.map.put("a", "fromBackend");
        DataStoreImpl<String, String> store = newStore(backend);
        assertEquals(Optional.of("fromBackend"), store.find("a").join());
    }

    @Test
    void find_absent_isEmpty() {
        assertEquals(Optional.empty(), newStore(new FakeBackend<>()).find("missing").join());
    }

    @Test
    void getOrCreate_createsWhenAbsent() {
        DataStoreImpl<String, String> store = newStore(new FakeBackend<>());
        assertEquals("new", store.getOrCreate("a", () -> "new").join());
        assertEquals(Optional.of("new"), store.find("a").join());
    }

    @Test
    void getOrCreate_returnsExisting() {
        DataStoreImpl<String, String> store = newStore(new FakeBackend<>());
        store.save("a", "existing").join();
        assertEquals("existing", store.getOrCreate("a", () -> "new").join());
    }

    @Test
    void update_appliesMutatorAndStores() {
        DataStoreImpl<String, String> store = newStore(new FakeBackend<>());
        store.save("a", "x").join();
        assertEquals("xy", store.update("a", v -> v + "y").join());
        assertEquals(Optional.of("xy"), store.find("a").join());
    }

    @Test
    void update_absent_completesExceptionally() {
        DataStoreImpl<String, String> store = newStore(new FakeBackend<>());
        assertThrows(CompletionException.class, () -> store.update("missing", v -> v).join());
    }

    @Test
    void delete_removesFromBothTiers() {
        FakeBackend<String> backend = new FakeBackend<>();
        DataStoreImpl<String, String> store = newStore(backend);
        store.save("a", "1").join();
        store.delete("a").join();
        assertFalse(store.contains("a").join());
        assertFalse(backend.map.containsKey("a"));
    }

    @Test
    void flush_pushesCacheToBackend() {
        FakeBackend<String> backend = new FakeBackend<>();
        MemoryCache<String> cache = new MemoryCache<>();
        DataStoreImpl<String, String> store = new DataStoreImpl<>(
                KeyMapper.string(), cache, backend, new LocalLockService());
        cache.put("a", "cached").join(); // simulate a cache-only entry
        store.flush().join();
        assertEquals("cached", backend.map.get("a"));
    }

    @Test
    void all_mergesCacheAndBackendCacheWins() {
        FakeBackend<String> backend = new FakeBackend<>();
        backend.map.put("a", "backendA");
        backend.map.put("b", "backendB");
        MemoryCache<String> cache = new MemoryCache<>();
        DataStoreImpl<String, String> store = new DataStoreImpl<>(
                KeyMapper.string(), cache, backend, new LocalLockService());
        cache.put("a", "cacheA").join();
        Map<String, String> all = store.all().join();
        assertEquals("cacheA", all.get("a"));
        assertEquals("backendB", all.get("b"));
        assertTrue(all.containsKey("a") && all.containsKey("b"));
    }

    @Test
    void load_fromBackendPopulatesCacheAndReturnsValue() {
        FakeBackend<String> backend = new FakeBackend<>();
        backend.map.put("a", "stored");
        DataStoreImpl<String, String> store = newStore(backend);
        assertEquals(Optional.of("stored"), store.load("a").join());
        assertTrue(store.contains("a").join());
    }

    @Test
    void load_absent_isEmpty() {
        assertEquals(Optional.empty(), newStore(new FakeBackend<>()).load("missing").join());
    }

    @Test
    void contains_fallsBackToBackend() {
        FakeBackend<String> backend = new FakeBackend<>();
        backend.map.put("a", "1");
        assertTrue(newStore(backend).contains("a").join());
    }

    @Test
    void getOrCreate_concurrentSameKey_callsFactoryOnce() throws Exception {
        DataStoreImpl<String, String> store = newStore(new FakeBackend<>());
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> factory = () -> { calls.incrementAndGet(); return "v"; };
        CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> store.getOrCreate("k", factory).join());
        CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> store.getOrCreate("k", factory).join());
        CompletableFuture.allOf(f1, f2).get();
        assertEquals(1, calls.get(), "factory must run exactly once for concurrent getOrCreate on the same key");
    }
}
