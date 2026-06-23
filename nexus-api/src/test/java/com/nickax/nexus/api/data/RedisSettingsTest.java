package com.nickax.nexus.api.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RedisSettingsTest {

    @Test
    void address_buildsRedisUri() {
        RedisSettings settings = new RedisSettings("localhost", 6379, null, 0);
        assertEquals("redis://localhost:6379", settings.address());
    }

    @Test
    void password_isOptionalAndExposed() {
        assertNull(new RedisSettings("h", 1, null, 0).password());
        assertEquals("secret", new RedisSettings("h", 1, "secret", 0).password());
    }
}
