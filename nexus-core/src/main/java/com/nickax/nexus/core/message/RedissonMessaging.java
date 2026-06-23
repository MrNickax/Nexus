package com.nickax.nexus.core.message;

import com.nickax.nexus.api.message.Channel;
import com.nickax.nexus.api.message.Messaging;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * {@link Messaging} over Redisson. Channels are topics named {@code nexus:channel:<name>};
 * the once-reservation uses buckets named {@code nexus:once:<id>}.
 */
public final class RedissonMessaging implements Messaging {

    private final RedissonClient redisson;
    private final String nodeId;
    private final Executor executor;

    /**
     * Creates the messaging service.
     *
     * @param redisson the shared client
     * @param nodeId   the local node id
     * @param executor the executor handlers run on
     */
    public RedissonMessaging(@NotNull RedissonClient redisson, @NotNull String nodeId, @NotNull Executor executor) {
        this.redisson = redisson;
        this.nodeId = nodeId;
        this.executor = executor;
    }

    /**
     * Returns a {@link RedissonChannel} over the Redisson topic
     * {@code nexus:channel:<name>}. Multiple calls with the same name return
     * independent channel objects backed by the same topic.
     *
     * @param name the channel name
     * @param type the payload class for deserialization
     * @return the channel
     */
    @Override
    public <T> @NotNull Channel<T> channel(@NotNull String name, @NotNull Class<T> type) {
        return new RedissonChannel<>(redisson.getTopic("nexus:channel:" + name), type, nodeId, executor);
    }

    /**
     * Attempts a distributed SETNX on the key {@code nexus:once:<operationId>}.
     * If this node wins the reservation (the key was not already set), {@code action}
     * is run on the supplied executor and the method returns {@code true}.
     * If another node already reserved the id the action is skipped and the method
     * returns {@code false}. The key expires after {@code ttl} so the operation can
     * be re-triggered once the window passes.
     *
     * @param operationId a unique identifier for this cross-server operation
     * @param ttl         how long the reservation key lives in Redis
     * @param action      the side effect to run if this node wins
     * @return a future resolving to {@code true} if the action was run by this node
     */
    @Override
    public @NotNull CompletableFuture<Boolean> once(@NotNull String operationId, @NotNull Duration ttl, @NotNull Runnable action) {
        RBucket<String> bucket = redisson.getBucket("nexus:once:" + operationId);
        return bucket.setIfAbsentAsync(nodeId, ttl).toCompletableFuture()
                .thenApplyAsync(reserved -> {
                    if (reserved) {
                        action.run();
                    }
                    return reserved;
                }, executor);
    }
}
