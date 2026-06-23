package com.nickax.nexus.core.config;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapConfigSectionTest {

    private MapConfigSection section() {
        return new MapConfigSection(new LinkedHashMap<>());
    }

    @Test
    void set_thenGet_topLevel() {
        MapConfigSection s = section();
        s.set("name", "nexus");
        assertEquals("nexus", s.getString("name"));
    }

    @Test
    void set_thenGet_nestedPath() {
        MapConfigSection s = section();
        s.set("limits.max", 10);
        assertEquals(10, s.getInt("limits.max"));
        assertTrue(s.contains("limits.max"));
        assertTrue(s.contains("limits"));
    }

    @Test
    void getInt_withDefault_whenAbsent() {
        assertEquals(5, section().getInt("missing", 5));
    }

    @Test
    void getString_absent_isNull() {
        assertNull(section().getString("missing"));
    }

    @Test
    void getBoolean_andDouble_andLong() {
        MapConfigSection s = section();
        s.set("a.flag", true);
        s.set("a.ratio", 1.5);
        s.set("a.big", 9000000000L);
        assertTrue(s.getBoolean("a.flag"));
        assertEquals(1.5, s.getDouble("a.ratio"));
        assertEquals(9000000000L, s.getLong("a.big"));
    }

    @Test
    void getStringList_returnsList_orEmpty() {
        MapConfigSection s = section();
        s.set("words", List.of("a", "b"));
        assertEquals(List.of("a", "b"), s.getStringList("words"));
        assertTrue(s.getStringList("missing").isEmpty());
    }

    @Test
    void getSection_returnsNestedView() {
        MapConfigSection s = section();
        s.set("group.x", 1);
        s.set("group.y", 2);
        var sub = s.getSection("group");
        assertEquals(1, sub.getInt("x"));
        assertEquals(2, sub.getInt("y"));
    }

    @Test
    void keys_returnsImmediateChildren() {
        MapConfigSection s = section();
        s.set("a", 1);
        s.set("b.c", 2);
        assertEquals(java.util.Set.of("a", "b"), s.keys());
    }

    @Test
    void set_null_removesKey() {
        MapConfigSection s = section();
        s.set("a", 1);
        s.set("a", null);
        assertFalse(s.contains("a"));
    }

    @Test
    void getInt_coercesNumberAndString() {
        MapConfigSection s = section();
        s.set("n", 7);
        s.set("str", "42");
        assertEquals(7, s.getInt("n"));
        assertEquals(42, s.getInt("str"));
    }

    @Test
    void set_onScalarIntermediate_throws() {
        MapConfigSection s = section();
        s.set("a", "hello");
        assertThrows(IllegalArgumentException.class, () -> s.set("a.b", 1));
    }

    @Test
    void contains_trueForExplicitNullValue() {
        java.util.Map<String, Object> backing = new java.util.LinkedHashMap<>();
        backing.put("k", null);
        MapConfigSection s = new MapConfigSection(backing);
        assertTrue(s.contains("k"));
    }
}
