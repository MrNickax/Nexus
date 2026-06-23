package com.nickax.nexus.api.data;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyMapperTest {

    @Test
    void uuid_roundTrips() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        KeyMapper<UUID> mapper = KeyMapper.uuid();
        assertEquals(id, mapper.fromKey(mapper.toKey(id)));
        assertEquals("00000000-0000-0000-0000-000000000001", mapper.toKey(id));
    }

    @Test
    void string_isIdentity() {
        KeyMapper<String> mapper = KeyMapper.string();
        assertEquals("abc", mapper.toKey("abc"));
        assertEquals("abc", mapper.fromKey("abc"));
    }

    @Test
    void integer_roundTrips() {
        KeyMapper<Integer> mapper = KeyMapper.integer();
        assertEquals(42, mapper.fromKey(mapper.toKey(42)));
        assertEquals("42", mapper.toKey(42));
    }

    @Test
    void longKey_roundTrips() {
        KeyMapper<Long> mapper = KeyMapper.longKey();
        assertEquals(42L, mapper.fromKey(mapper.toKey(42L)));
    }
}
