package com.nickax.nexus.api.lock;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Named mutual-exclusion locks. The local implementation guards within one JVM;
 * the Redis implementation (a later plan) guards across servers. Same API either way.
 */
public interface LockService {

    /**
     * Runs an async action while holding the named lock, releasing it when the
     * action's future completes (normally or exceptionally).
     *
     * @param key    the lock key
     * @param action supplies the future to run under the lock
     * @param <T>    the result type
     * @return a future of the action's result
     */
    <T> @NotNull CompletableFuture<T> withLock(@NotNull String key, @NotNull Supplier<CompletableFuture<T>> action);

    /**
     * Acquires the named lock, blocking until held, and returns the lease.
     * Intended for try-with-resources in synchronous code.
     *
     * @param key the lock key
     * @return the held lease
     */
    @NotNull Lock acquire(@NotNull String key);
}
