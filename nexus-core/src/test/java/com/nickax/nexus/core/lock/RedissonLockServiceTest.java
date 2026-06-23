package com.nickax.nexus.core.lock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RedissonLockServiceTest {

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

    @Test
    void withLock_runsActionAndReturnsResult() {
        RedissonLockService locks = new RedissonLockService(client);
        assertEquals("done", locks.withLock("k", () -> CompletableFuture.completedFuture("done")).join());
    }

    @Test
    void withLock_releasesWhenActionFutureFails() {
        RedissonLockService locks = new RedissonLockService(client);
        assertThrows(CompletionException.class, () ->
                locks.withLock("k", () -> CompletableFuture.<String>failedFuture(new RuntimeException("boom"))).join());
        assertEquals("ok", locks.withLock("k", () -> CompletableFuture.completedFuture("ok")).join());
    }

    @Test
    void withLock_serialisesConcurrentAccessToSameKey() throws Exception {
        RedissonLockService locks = new RedissonLockService(client);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxObserved = new AtomicInteger();
        CompletableFuture<?>[] futures = new CompletableFuture[6];
        for (int i = 0; i < futures.length; i++) {
            futures[i] = locks.withLock("same", () -> CompletableFuture.supplyAsync(() -> {
                int now = active.incrementAndGet();
                maxObserved.accumulateAndGet(now, Math::max);
                try { Thread.sleep(15); } catch (InterruptedException ignored) { }
                active.decrementAndGet();
                return null;
            }));
        }
        CompletableFuture.allOf(futures).get();
        assertEquals(1, maxObserved.get(), "same-key holders must not overlap");
    }
}
