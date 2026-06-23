package com.nickax.nexus.core.lock;

import com.nickax.nexus.api.lock.Lock;
import com.nickax.nexus.api.lock.LockService;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * In-JVM {@link LockService}. Holds one binary {@link Semaphore} per key. A
 * semaphore (not a {@code ReentrantLock}) is used because a lock acquired on one
 * thread may be released on another when an action's future completes on an
 * executor thread, which a reentrant lock forbids.
 *
 * <p>Known limitations: the per-key map is never evicted (acceptable for bounded
 * key spaces such as player UUIDs), and the locks are NOT reentrant. A Redis
 * implementation in a later plan provides cross-server locking behind the same API.
 */
public final class LocalLockService implements LockService {

    private final ConcurrentHashMap<String, Semaphore> locks = new ConcurrentHashMap<>();

    /**
     * Returns the semaphore for the given key, creating one on first access.
     * The map is never evicted; this is intentional for bounded key spaces
     * (e.g. player UUIDs) where the set of distinct keys is small and stable.
     *
     * @param key the lock key
     * @return the binary semaphore for that key
     */
    private Semaphore lockFor(String key) {
        return locks.computeIfAbsent(key, k -> new Semaphore(1));
    }

    /**
     * Acquires the semaphore for {@code key}, runs {@code action}, and releases the
     * semaphore when the returned future completes (whether normally or exceptionally).
     * If the thread is interrupted while waiting for the lock, the future is failed
     * with the {@link InterruptedException} and the interrupt flag is restored.
     *
     * @param key    the lock key
     * @param action the operation to run under the lock
     * @return a future that completes with the action's result
     */
    @Override
    public <T> @NotNull CompletableFuture<T> withLock(@NotNull String key,
                                                      @NotNull Supplier<CompletableFuture<T>> action) {
        Semaphore semaphore = lockFor(key);
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }
        CompletableFuture<T> future;
        try {
            future = action.get();
        } catch (Throwable t) {
            semaphore.release();
            return CompletableFuture.failedFuture(t);
        }
        return future.whenComplete((result, error) -> semaphore.release());
    }

    /**
     * Blocks until the semaphore for {@code key} is acquired and returns a
     * {@link Lock} whose {@code close()} releases it. Suitable for try-with-resources
     * in synchronous code; the returned lock is not reentrant.
     *
     * @param key the lock key
     * @return an open lock; the caller must close it
     * @throws IllegalStateException if the thread is interrupted while waiting
     */
    @Override
    public @NotNull Lock acquire(@NotNull String key) {
        Semaphore semaphore = lockFor(key);
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while acquiring lock " + key, e);
        }
        return semaphore::release;
    }
}
