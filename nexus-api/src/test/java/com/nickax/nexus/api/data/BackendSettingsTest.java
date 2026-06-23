package com.nickax.nexus.api.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackendSettingsTest {

    @Test
    void mongoSettings_exposeConnectionAndDatabase() {
        MongoSettings settings = new MongoSettings("mongodb://localhost:27017", "nexus");
        assertEquals("mongodb://localhost:27017", settings.connectionString());
        assertEquals("nexus", settings.database());
    }

    @Test
    void sqlSettings_exposeFields() {
        SqlSettings settings = new SqlSettings("jdbc:mysql://localhost/nexus", "root", "pw", 10);
        assertEquals("jdbc:mysql://localhost/nexus", settings.jdbcUrl());
        assertEquals("root", settings.username());
        assertEquals("pw", settings.password());
        assertEquals(10, settings.poolSize());
    }
}
