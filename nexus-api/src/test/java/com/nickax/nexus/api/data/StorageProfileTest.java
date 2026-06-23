package com.nickax.nexus.api.data;

import com.nickax.nexus.api.config.ConfigSection;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class StorageProfileTest {

    /** Minimal in-memory ConfigSection stub backed by a flat Map (only the getters used by from()). */
    static ConfigSection section(Map<String, Object> map) {
        return new ConfigSection() {

            @Override
            public boolean contains(String path) {
                return map.containsKey(path);
            }

            @Override
            public Object get(String path) {
                return map.get(path);
            }

            @Override
            public String getString(String path) {
                Object v = map.get(path);
                return v != null ? v.toString() : null;
            }

            @Override
            public String getString(String path, String def) {
                Object v = map.get(path);
                return v != null ? v.toString() : def;
            }

            @Override
            public int getInt(String path) {
                Object v = map.get(path);
                return v instanceof Number n ? n.intValue() : 0;
            }

            @Override
            public int getInt(String path, int def) {
                Object v = map.get(path);
                return v instanceof Number n ? n.intValue() : def;
            }

            @Override
            @SuppressWarnings("unchecked")
            public ConfigSection getSection(String path) {
                Object v = map.get(path);
                if (v instanceof Map<?, ?> nested) {
                    return section((Map<String, Object>) nested);
                }
                return null;
            }

            // Unused methods — only those called by StorageProfile.from() are implemented above.

            @Override public long getLong(String path) { throw new UnsupportedOperationException(); }
            @Override public long getLong(String path, long def) { throw new UnsupportedOperationException(); }
            @Override public double getDouble(String path) { throw new UnsupportedOperationException(); }
            @Override public double getDouble(String path, double def) { throw new UnsupportedOperationException(); }
            @Override public boolean getBoolean(String path) { throw new UnsupportedOperationException(); }
            @Override public boolean getBoolean(String path, boolean def) { throw new UnsupportedOperationException(); }
            @Override public List<String> getStringList(String path) { throw new UnsupportedOperationException(); }
            @Override public Set<String> keys() { throw new UnsupportedOperationException(); }
            @Override public void set(String path, Object value) { throw new UnsupportedOperationException(); }
        };
    }

    @Test
    void from_readsMongoMemoryProfile() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("backend", "mongo");
        root.put("cache", "memory");
        Map<String, Object> mongo = new LinkedHashMap<>();
        mongo.put("connection-string", "mongodb://localhost:27017");
        mongo.put("database", "economy");
        root.put("mongo", mongo);

        StorageProfile profile = StorageProfile.from(section(root));
        assertEquals(StorageProfile.Backend.MONGO, profile.backend());
        assertEquals(StorageProfile.Cache.MEMORY, profile.cache());
        assertNotNull(profile.mongo());
        assertEquals("economy", profile.mongo().database());
        assertNull(profile.sql());
        assertNull(profile.redis());
    }

    @Test
    void from_readsFileBackend_default() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("backend", "file");
        StorageProfile profile = StorageProfile.from(section(root));
        assertEquals(StorageProfile.Backend.FILE, profile.backend());
        assertEquals(StorageProfile.Cache.MEMORY, profile.cache()); // default
    }

    @Test
    void from_mongoBackendWithoutSection_throws() {
        java.util.Map<String, Object> root = new java.util.LinkedHashMap<>();
        root.put("backend", "mongo"); // no mongo section
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> StorageProfile.from(section(root)));
    }

    @Test
    void from_invalidBackend_throws() {
        java.util.Map<String, Object> root = new java.util.LinkedHashMap<>();
        root.put("backend", "mangodb");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> StorageProfile.from(section(root)));
    }
}
