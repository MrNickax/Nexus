package com.nickax.nexus.common.storage;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages a dedicated thread pool executor for JSON storage I/O operations.
 * <p>
 * This class provides a centralized executor service optimized for asynchronous JSON storage operations.
 * The thread pool size is automatically calculated based on available processors, with a minimum of 2
 * and a maximum of 4 threads. All threads are daemon threads to prevent blocking JVM shutdown.
 * <p>
 * Thread naming follows the pattern: "JsonStorage-IO-{id}"
 */
public class JsonStorageExecutor {

    private static final AtomicInteger jsonThreadId = new AtomicInteger(1);
    private static ExecutorService jsonExecutor;

    static {
        int threads = Math.min(
                4,
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2)
        );

        JsonStorageExecutor.jsonExecutor = Executors.newFixedThreadPool(
                threads,
                r -> {
                    Thread t = new Thread(r, "JsonStorage-IO-" + jsonThreadId.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
        );
    }

    /**
     * Returns the shared executor for JSON storage operations.
     * <p>
     * This executor should be used for all asynchronous JSON read/write operations
     * to ensure efficient thread management and prevent resource exhaustion.
     *
     * @return the JSON storage executor instance
     */
    public static Executor getJsonExecutor() {
        return jsonExecutor;
    }

    /**
     * Initiates an orderly shutdown of the executor service.
     * <p>
     * This method attempts to gracefully shut down the executor by:
     * <ol>
     *   <li>Calling {@code shutdown()} to prevent new task submissions</li>
     *   <li>Waiting up to 5 seconds for existing tasks to complete</li>
     *   <li>Forcing shutdown with {@code shutdownNow()} if tasks don't complete in time</li>
     * </ol>
     * If interrupted during shutdown, the current thread's interrupted status is preserved.
     */
    public static void shutdown() {
        jsonExecutor.shutdown();
        try {
            if (!jsonExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                jsonExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            jsonExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}