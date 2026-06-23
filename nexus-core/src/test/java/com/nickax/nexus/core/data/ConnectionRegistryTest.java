package com.nickax.nexus.core.data;

import com.nickax.nexus.api.data.RedisSettings;
import com.nickax.nexus.api.data.SqlSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import redis.embedded.RedisServer;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConnectionRegistryTest {

    private RedisServer redis;
    private int port;
    private ConnectionRegistry registry;

    private static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) { return s.getLocalPort(); }
    }

    @BeforeEach
    void start() throws IOException {
        port = freePort();
        redis = RedisServer.newRedisServer().port(port).build();
        redis.start();
        registry = new ConnectionRegistry();
    }

    @AfterEach
    void stop() throws IOException {
        registry.closeAll();
        redis.stop();
    }

    @Test
    void redisson_sameSettings_returnsSameClient() {
        RedisSettings settings = new RedisSettings("127.0.0.1", port, null, 0);
        RedissonClient a = registry.redisson(settings);
        RedissonClient b = registry.redisson(settings);
        assertSame(a, b);
    }

    @Test
    void redisson_differentSettings_returnsDifferentClients() throws IOException {
        int port2 = freePort();
        RedisServer redis2 = RedisServer.newRedisServer().port(port2).build();
        redis2.start();
        try {
            RedissonClient a = registry.redisson(new RedisSettings("127.0.0.1", port, null, 0));
            RedissonClient b = registry.redisson(new RedisSettings("127.0.0.1", port2, null, 0));
            assertNotSame(a, b);
        } finally {
            redis2.stop();
        }
    }

    @Test
    void sql_sameSettings_returnsSameDataSource() {
        SqlSettings settings = new SqlSettings(
                "jdbc:h2:mem:reg" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", null, 2);
        DataSource a = registry.sql(settings);
        DataSource b = registry.sql(settings);
        assertSame(a, b);
        assertNotNull(a);
    }

    @Test
    void closeAll_shutsDownClients() {
        RedissonClient client = registry.redisson(new RedisSettings("127.0.0.1", port, null, 0));
        registry.closeAll();
        org.junit.jupiter.api.Assertions.assertTrue(client.isShutdown());
    }
}
