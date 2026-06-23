package com.nickax.nexus.core.data;

import com.nickax.nexus.api.data.Codec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisCacheTest {

    private static final Codec<String> CODEC = new Codec<>() {
        @Override public String encode(String value) { return value; }
        @Override public String decode(String data) { return data; }
    };
    private static final Executor DIRECT = Runnable::run;

    private RedisServer server;
    private RedissonClient client;

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) { return socket.getLocalPort(); }
    }

    @BeforeEach
    void start() throws IOException {
        int port = freePort();
        server = RedisServer.newRedisServer().port(port).build();
        server.start();
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:" + port);
        client = Redisson.create(config);
    }

    @AfterEach
    void stop() throws IOException {
        if (client != null) client.shutdown();
        if (server != null) server.stop();
    }

    private RedisCache<String> cache() {
        return new RedisCache<>(client.getMap("test"), CODEC, DIRECT);
    }

    @Test
    void put_thenGet_roundTrips() {
        RedisCache<String> cache = cache();
        cache.put("a", "1").join();
        assertEquals(Optional.of("1"), cache.get("a").join());
    }

    @Test
    void get_absent_isEmpty() {
        assertEquals(Optional.empty(), cache().get("missing").join());
    }

    @Test
    void remove_deletesEntry() {
        RedisCache<String> cache = cache();
        cache.put("a", "1").join();
        cache.remove("a").join();
        assertFalse(cache.contains("a").join());
    }

    @Test
    void contains_reflectsPresence() {
        RedisCache<String> cache = cache();
        assertFalse(cache.contains("a").join());
        cache.put("a", "1").join();
        assertTrue(cache.contains("a").join());
    }

    @Test
    void all_returnsEveryEntry() {
        RedisCache<String> cache = cache();
        cache.put("a", "1").join();
        cache.put("b", "2").join();
        assertEquals(Map.of("a", "1", "b", "2"), cache.all().join());
    }
}
