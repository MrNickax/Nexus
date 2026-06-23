package com.nickax.nexus.core.data;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryCacheTest {

    @Test
    void put_thenGet_returnsValue() {
        MemoryCache<String> cache = new MemoryCache<>();
        cache.put("a", "1").join();
        assertEquals(Optional.of("1"), cache.get("a").join());
    }

    @Test
    void get_absent_isEmpty() {
        MemoryCache<String> cache = new MemoryCache<>();
        assertEquals(Optional.empty(), cache.get("missing").join());
    }

    @Test
    void remove_deletesEntry() {
        MemoryCache<String> cache = new MemoryCache<>();
        cache.put("a", "1").join();
        cache.remove("a").join();
        assertFalse(cache.contains("a").join());
    }

    @Test
    void contains_reflectsPresence() {
        MemoryCache<String> cache = new MemoryCache<>();
        assertFalse(cache.contains("a").join());
        cache.put("a", "1").join();
        assertTrue(cache.contains("a").join());
    }

    @Test
    void all_returnsSnapshotCopy() {
        MemoryCache<String> cache = new MemoryCache<>();
        cache.put("a", "1").join();
        cache.put("b", "2").join();
        Map<String, String> all = cache.all().join();
        assertEquals(Map.of("a", "1", "b", "2"), all);
        all.clear(); // must not affect the cache
        assertTrue(cache.contains("a").join());
    }
}
