package com.nickax.nexus.core.lock;

import com.nickax.nexus.api.lock.Lock;
import com.nickax.nexus.api.lock.LockService;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Cross-server {@link LockService} via Redisson {@link RLock}. A unique lock id is
 * captured per acquisition and passed to both {@code lockAsync} and
 * {@code unlockAsync}, so release succeeds even when the releasing thread differs
 * from the acquiring thread (the norm in async code). Same API as the local service.
 */
public final class RedissonLockService implements LockService {

    private final RedissonClient redisson;
    private final AtomicLong ids = new AtomicLong();
    private final System.Logger logger = System.getLogger("Nexus");

    /**
     * Creates the service over a Redisson client.
     *
     * @param redisson the shared Redisson client
     */
    public RedissonLockService(@NotNull RedissonClient redisson) {
        this.redisson = redisson;
    }

    /**
     * Acquires the distributed lock for {@code key}, runs {@code action}, and
     * unconditionally releases the lock when the action's future settles. The unique
     * per-acquisition {@code lockId} lets Redisson identify the holder across threads,
     * which is necessary for async code where acquire and release happen on different
     * threads.
     *
     * @param key    the lock key (used as the Redisson lock name)
     * @param action the operation to run under the lock
     * @return a future that completes with the action's result
     */
    @Override
    public <T> @NotNull CompletableFuture<T> withLock(@NotNull String key,
                                                      @NotNull Supplier<CompletableFuture<T>> action) {
        RLock lock = redisson.getLock(key);
        long lockId = ids.incrementAndGet();
        return lock.lockAsync(lockId).toCompletableFuture().thenCompose(ignored -> {
            CompletableFuture<T> actionFuture;
            try {
                actionFuture = action.get();
            } catch (Throwable t) {
                actionFuture = CompletableFuture.failedFuture(t);
            }
            return actionFuture
                    .handle((result, error) -> (Map.Entry<T, Throwable>) new AbstractMap.SimpleImmutableEntry<>(result, error))
                    .thenCompose(outcome -> releaseThen(lock, lockId, outcome));
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>This blocks until the lock is held. Do not call it from a Netty event-loop
     * or a Redisson completion callback (it would deadlock); it is safe on virtual
     * threads and ordinary threads.
     */
    @Override
    public @NotNull Lock acquire(@NotNull String key) {
        RLock lock = redisson.getLock(key);
        long lockId = ids.incrementAndGet();
        lock.lockAsync(lockId).toCompletableFuture().join();
        return () -> lock.unlockAsync(lockId).toCompletableFuture().join();
    }

    /**
     * Releases the distributed lock and then propagates {@code outcome}: completes
     * normally with the result or fails with the error. A failed unlock is logged as a
     * warning and swallowed — Redisson's watchdog will expire the lock eventually.
     *
     * @param lock    the lock to release
     * @param lockId  the per-acquisition id used to unlock the correct holder
     * @param outcome the action result (value + error pair)
     * @return a future that propagates the original action outcome
     */
    private <T> CompletableFuture<T> releaseThen(RLock lock, long lockId, Map.Entry<T, Throwable> outcome) {
        return lock.unlockAsync(lockId).toCompletableFuture()
                .exceptionally(error -> {
                    logger.log(System.Logger.Level.WARNING,
                            "Failed to release distributed lock; it will expire via the watchdog", error);
                    return null;
                })
                .thenCompose(ignored -> {
                    Throwable error = outcome.getValue();
                    if (error != null) {
                        return CompletableFuture.failedFuture(error);
                    }
                    return CompletableFuture.completedFuture(outcome.getKey());
                });
    }
}
