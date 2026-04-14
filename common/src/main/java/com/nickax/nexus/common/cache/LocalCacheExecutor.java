package com.nickax.nexus.common.cache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages executor services for local cache operations.
 * <p>
 * This class provides centralized thread pool management for cache-related tasks,
 * including asynchronous cache operations and scheduled cleanup tasks. All threads
 * created are daemon threads to prevent blocking JVM shutdown.
 */
public class LocalCacheExecutor {

    private static final AtomicInteger cacheThreadId = new AtomicInteger(1);
    private static final AtomicInteger cleanupThreadId = new AtomicInteger(1);

    private static final ExecutorService cacheExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "LocalCache-Async-" + cacheThreadId.getAndIncrement());
                t.setDaemon(true);
                return t;
            });

    private static final ScheduledExecutorService cleanupExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "LocalCache-Cleanup-" + cleanupThreadId.getAndIncrement());
                t.setDaemon(true);
                return t;
            });

    /**
     * Returns the executor service for asynchronous cache operations.
     *
     * @return the cache operation executor
     */
    public static ExecutorService getCacheExecutor() {
        return cacheExecutor;
    }

    /**
     * Returns the scheduled executor service for cache cleanup tasks.
     *
     * @return the cleanup task executor
     */
    public static ScheduledExecutorService getCleanupExecutor() {
        return cleanupExecutor;
    }

    /**
     * Gracefully shuts down both executor services.
     * <p>
     * Initiates an orderly shutdown and waits up to 5 seconds for each executor
     * to terminate. If an executor does not terminate within the timeout period,
     * it is forcefully shut down. If interrupted during shutdown, both executors
     * are forcefully shut down and the interrupt status is restored.
     */
    public static void shutdown() {
        cacheExecutor.shutdown();
        cleanupExecutor.shutdown();

        try {
            if (!cacheExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cacheExecutor.shutdownNow();
            }
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cacheExecutor.shutdownNow();
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}