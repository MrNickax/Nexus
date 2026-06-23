package com.nickax.nexus.core.webhook;

import com.nickax.nexus.api.webhook.Embed;
import com.nickax.nexus.api.webhook.WebhookMessage;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpWebhookTest {

    private HttpServer server;
    private HttpClient client;
    private final BlockingQueue<String> bodies = new ArrayBlockingQueue<>(4);

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            bodies.add(body);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    private String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/hook";
    }

    @Test
    void send_postsJsonBody() throws Exception {
        HttpWebhook webhook = new HttpWebhook(url(), client);
        webhook.send(WebhookMessage.builder()
                .content("hello")
                .embed(Embed.builder().title("T").description("D").color(0x44FF00).build())
                .build()).join();

        String body = bodies.poll(5, TimeUnit.SECONDS);
        assertNotNull(body, "no request received");
        assertTrue(body.contains("\"content\""));
        assertTrue(body.contains("hello"));
        assertTrue(body.contains("\"embeds\""));
        assertTrue(body.contains("\"title\""));
    }

    @Test
    void send_failsFutureOnErrorResponse() {
        server.createContext("/bad", exchange -> {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
        });
        HttpWebhook webhook = new HttpWebhook(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/bad", client);
        assertThrows(CompletionException.class,
                () -> webhook.send(WebhookMessage.builder().content("x").build()).join());
    }

    @Test
    void emptyMessage_throwsAtBuild() {
        assertThrows(IllegalStateException.class, () -> WebhookMessage.builder().build());
    }
}
