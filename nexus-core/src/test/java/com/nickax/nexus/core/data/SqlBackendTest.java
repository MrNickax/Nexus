package com.nickax.nexus.core.data;

import com.nickax.nexus.api.data.Codec;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SqlBackendTest {

    private static final Codec<String> CODEC = new Codec<>() {
        @Override public String encode(String value) { return value; }
        @Override public String decode(String data) { return data; }
    };
    private static final Executor DIRECT = Runnable::run;

    private DataSource dataSource;

    @BeforeEach
    void freshDb() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        dataSource = ds;
    }

    private SqlBackend<String> backend() {
        return new SqlBackend<>(dataSource, "store", CODEC, DIRECT);
    }

    @Test
    void put_thenGet_roundTrips() {
        SqlBackend<String> backend = backend();
        backend.put("a", "1").join();
        assertEquals(Optional.of("1"), backend.get("a").join());
    }

    @Test
    void put_overwrites() {
        SqlBackend<String> backend = backend();
        backend.put("a", "1").join();
        backend.put("a", "2").join();
        assertEquals(Optional.of("2"), backend.get("a").join());
    }

    @Test
    void get_absent_isEmpty() {
        assertEquals(Optional.empty(), backend().get("missing").join());
    }

    @Test
    void remove_deletesEntry() {
        SqlBackend<String> backend = backend();
        backend.put("a", "1").join();
        backend.remove("a").join();
        assertFalse(backend.contains("a").join());
    }

    @Test
    void all_readsEveryEntry() {
        SqlBackend<String> backend = backend();
        backend.put("a", "1").join();
        backend.put("b", "2").join();
        assertEquals(Map.of("a", "1", "b", "2"), backend.all().join());
    }
}
