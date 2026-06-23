package com.nickax.nexus.core.message;

import com.google.gson.Gson;
import com.nickax.nexus.api.message.Channel;
import com.nickax.nexus.api.message.MessageContext;
import com.nickax.nexus.api.message.MessageHandler;
import com.nickax.nexus.api.message.Subscription;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A {@link Channel} over a Redisson {@link RTopic}. Each message is a JSON
 * envelope {@code {"node":<id>,"payload":<json>}} so subscribers can tell who sent
 * it. Handler invocation hops to the supplied executor off Redisson's threads.
 *
 * @param <T> the payload type
 */
public final class RedissonChannel<T> implements Channel<T> {

    private static final Gson GSON = new Gson();
    private static final System.Logger LOGGER = System.getLogger("Nexus");

    private final RTopic topic;
    private final Class<T> type;
    private final String nodeId;
    private final Executor executor;

    /**
     * Package-private constructor; use {@link RedissonMessaging#channel} instead.
     *
     * @param topic    the Redisson topic backing this channel
     * @param type     the payload class for Gson deserialization
     * @param nodeId   the local node id stamped on outgoing messages
     * @param executor the executor handler invocations run on
     */
    RedissonChannel(@NotNull RTopic topic, @NotNull Class<T> type, @NotNull String nodeId, @NotNull Executor executor) {
        this.topic = topic;
        this.type = type;
        this.nodeId = nodeId;
        this.executor = executor;
    }

    /**
     * JSON wire format for messages on this channel. Carries the sending node id
     * alongside the payload so subscribers can detect self-published messages.
     */
    private static final class Envelope {
        String node;
        String payload;
    }

    /**
     * Publishes {@code payload} as a JSON envelope. The subscriber count returned
     * by Redisson is discarded — this is fire-and-forget.
     *
     * @param payload the payload to publish
     * @return a future that completes when Redisson acknowledges the publish
     */
    @Override
    public @NotNull CompletableFuture<Void> publish(@NotNull T payload) {
        Envelope envelope = new Envelope();
        envelope.node = nodeId;
        envelope.payload = GSON.toJson(payload);
        // The publish future yields the subscriber count; we intentionally discard it (fire-and-forget).
        return topic.publishAsync(GSON.toJson(envelope)).toCompletableFuture().thenApply(count -> null);
    }

    /**
     * Attaches a listener that decodes each incoming envelope, builds a
     * {@link SimpleMessageContext} (including the {@code fromSelf} flag), and
     * invokes {@code handler} on the supplied executor to stay off Redisson's
     * Netty threads. Malformed envelopes and handler exceptions are logged but do
     * not remove the subscription.
     *
     * @param handler the handler to invoke for each message
     * @return a subscription whose {@code cancel()} removes the Redisson listener
     */
    @Override
    public @NotNull Subscription subscribe(@NotNull MessageHandler<T> handler) {
        MessageListener<String> listener = (channel, raw) -> {
            try {
                Envelope envelope = GSON.fromJson(raw, Envelope.class);
                if (envelope == null || envelope.node == null || envelope.payload == null) {
                    return;
                }
                T payload = GSON.fromJson(envelope.payload, type);
                MessageContext context = new SimpleMessageContext(envelope.node, envelope.node.equals(nodeId));
                executor.execute(() -> handler.handle(payload, context));
            } catch (RuntimeException e) {
                LOGGER.log(System.Logger.Level.WARNING, "Failed to handle message on a channel", e);
            }
        };
        int id = topic.addListener(String.class, listener);
        return () -> topic.removeListener(id);
    }
}
