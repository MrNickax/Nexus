package com.nickax.nexus.core.message;

import com.nickax.nexus.api.message.Channel;
import com.nickax.nexus.api.message.MessageContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedissonMessagingTest {

    record Greeting(String text) { }

    private RedisServer server;
    private RedissonClient client;

    private static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) { return s.getLocalPort(); }
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
    void publish_isReceivedBySubscriber_withSelfFlag() throws Exception {
        RedissonMessaging messaging = new RedissonMessaging(client, "nodeA", Runnable::run);
        Channel<Greeting> channel = messaging.channel("greet", Greeting.class);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Greeting> received = new AtomicReference<>();
        AtomicReference<MessageContext> ctx = new AtomicReference<>();
        channel.subscribe((msg, context) -> { received.set(msg); ctx.set(context); latch.countDown(); });

        channel.publish(new Greeting("hi")).join();

        assertTrue(latch.await(5, TimeUnit.SECONDS), "message not received");
        assertEquals("hi", received.get().text());
        assertEquals("nodeA", ctx.get().sourceNodeId());
        assertTrue(ctx.get().fromSelf());
    }

    @Test
    void message_fromAnotherNode_isNotFromSelf() throws Exception {
        RedissonMessaging publisher = new RedissonMessaging(client, "nodeA", Runnable::run);
        RedissonMessaging subscriber = new RedissonMessaging(client, "nodeB", Runnable::run);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<MessageContext> ctx = new AtomicReference<>();
        subscriber.channel("greet", Greeting.class).subscribe((m, c) -> { ctx.set(c); latch.countDown(); });
        publisher.channel("greet", Greeting.class).publish(new Greeting("hi")).join();

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("nodeA", ctx.get().sourceNodeId());
        assertFalse(ctx.get().fromSelf());
    }

    @Test
    void once_runsActionForFirstCaller_only() {
        RedissonMessaging messaging = new RedissonMessaging(client, "nodeA", Runnable::run);
        AtomicInteger runs = new AtomicInteger();
        String op = "op-" + System.nanoTime();

        boolean first = messaging.once(op, Duration.ofMinutes(1), runs::incrementAndGet).join();
        boolean second = messaging.once(op, Duration.ofMinutes(1), runs::incrementAndGet).join();

        assertTrue(first);
        assertFalse(second);
        assertEquals(1, runs.get());
    }
}
