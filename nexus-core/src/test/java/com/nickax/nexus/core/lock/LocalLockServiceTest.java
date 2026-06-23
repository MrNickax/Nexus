package com.nickax.nexus.core.lock;

import com.nickax.nexus.api.lock.Lock;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalLockServiceTest {

    @Test
    void withLock_runsActionAndReturnsResult() {
        LocalLockService locks = new LocalLockService();
        String result = locks.withLock("k", () -> CompletableFuture.completedFuture("done")).join();
        assertEquals("done", result);
    }

    @Test
    void withLock_serialisesConcurrentAccessToSameKey() throws Exception {
        LocalLockService locks = new LocalLockService();
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxObserved = new AtomicInteger();

        Runnable body = () -> {
            int now = active.incrementAndGet();
            maxObserved.accumulateAndGet(now, Math::max);
            try { Thread.sleep(20); } catch (InterruptedException ignored) { }
            active.decrementAndGet();
        };

        CompletableFuture<?>[] futures = new CompletableFuture[8];
        for (int i = 0; i < futures.length; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                try (Lock lock = locks.acquire("same")) {
                    body.run();
                }
            });
        }
        CompletableFuture.allOf(futures).get();

        assertEquals(1, maxObserved.get(), "no two holders of the same key may overlap");
    }

    @Test
    void acquire_differentKeys_doNotBlockEachOther() {
        LocalLockService locks = new LocalLockService();
        try (Lock a = locks.acquire("a"); Lock b = locks.acquire("b")) {
            assertNotNull(a);
            assertNotNull(b);
        }
    }

    @Test
    void withLock_releasesWhenActionFutureFails() {
        LocalLockService locks = new LocalLockService();
        assertThrows(java.util.concurrent.CompletionException.class, () ->
                locks.withLock("k", () ->
                        CompletableFuture.<String>failedFuture(new RuntimeException("boom"))).join());
        // lock must have been released; a follow-up acquisition on the same key must succeed
        assertEquals("ok", locks.withLock("k", () -> CompletableFuture.completedFuture("ok")).join());
    }
}
