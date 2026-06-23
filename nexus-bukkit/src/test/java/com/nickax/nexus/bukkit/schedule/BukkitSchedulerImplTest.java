package com.nickax.nexus.bukkit.schedule;

import com.nickax.nexus.api.schedule.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitSchedulerImplTest {

    private ServerMock server;
    private Plugin plugin;
    private BukkitSchedulerImpl scheduler;

    @BeforeEach
    void start() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("Test");
        scheduler = new BukkitSchedulerImpl(plugin);
    }

    @AfterEach
    void stop() {
        MockBukkit.unmock();
    }

    @Test
    void sync_runsOnTick() {
        AtomicInteger ran = new AtomicInteger();
        scheduler.sync(ran::incrementAndGet);
        server.getScheduler().performOneTick();
        assertEquals(1, ran.get());
    }

    @Test
    void syncLater_runsAfterDelay() {
        AtomicInteger ran = new AtomicInteger();
        scheduler.syncLater(Duration.ofMillis(100), ran::incrementAndGet); // 2 ticks
        server.getScheduler().performTicks(1);
        assertEquals(0, ran.get());
        server.getScheduler().performTicks(2);
        assertEquals(1, ran.get());
    }

    @Test
    void syncTimer_repeatsUntilCancelled() {
        AtomicInteger ran = new AtomicInteger();
        ScheduledTask task = scheduler.syncTimer(Duration.ZERO, Duration.ofMillis(50), ran::incrementAndGet);
        server.getScheduler().performTicks(3);
        assertTrue(ran.get() >= 2, "should have run multiple times");
        int at = ran.get();
        task.cancel();
        assertTrue(task.isCancelled());
        server.getScheduler().performTicks(3);
        assertEquals(at, ran.get(), "no more runs after cancel");
    }

    @Test
    void async_runsTheTask() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        scheduler.async(latch::countDown);
        try {
            ((org.mockbukkit.mockbukkit.scheduler.BukkitSchedulerMock) server.getScheduler()).waitAsyncTasksFinished();
        } catch (Throwable ignored) {
            // drain not available — fall through to latch timeout
        }
        assertTrue(latch.await(2, TimeUnit.SECONDS), "async task did not run");
    }

    @Test
    void region_fallsBackToMainThread() {
        WorldMock world = server.addSimpleWorld("w");
        Location location = new Location(world, 0, 64, 0);
        AtomicInteger ran = new AtomicInteger();
        scheduler.region(location, ran::incrementAndGet);
        server.getScheduler().performOneTick();
        assertEquals(1, ran.get());
    }

    @Test
    void entity_fallsBackToMainThread() {
        Player player = server.addPlayer();
        AtomicInteger ran = new AtomicInteger();
        scheduler.entity(player, ran::incrementAndGet);
        server.getScheduler().performOneTick();
        assertEquals(1, ran.get());
    }

    @Test
    void asyncLater_runsTheTask() throws Exception {
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        scheduler.asyncLater(java.time.Duration.ofMillis(100), latch::countDown);
        server.getScheduler().performTicks(3);
        try {
            ((org.mockbukkit.mockbukkit.scheduler.BukkitSchedulerMock) server.getScheduler()).waitAsyncTasksFinished();
        } catch (Throwable ignored) {
        }
        assertTrue(latch.await(2, java.util.concurrent.TimeUnit.SECONDS));
    }

    @Test
    void regionLater_fallsBackToMainThreadAfterDelay() {
        WorldMock world = server.addSimpleWorld("w");
        Location location = new Location(world, 0, 64, 0);
        AtomicInteger ran = new AtomicInteger();
        scheduler.regionLater(location, java.time.Duration.ofMillis(100), ran::incrementAndGet);
        server.getScheduler().performTicks(1);
        assertEquals(0, ran.get());
        server.getScheduler().performTicks(2);
        assertEquals(1, ran.get());
    }

    @Test
    void entityTimer_fallsBackToMainThread_repeats() {
        Player player = server.addPlayer();
        AtomicInteger ran = new AtomicInteger();
        ScheduledTask task =
                scheduler.entityTimer(player, java.time.Duration.ZERO, java.time.Duration.ofMillis(50), ran::incrementAndGet);
        server.getScheduler().performTicks(3);
        assertTrue(ran.get() >= 2);
        task.cancel();
        int at = ran.get();
        server.getScheduler().performTicks(3);
        assertEquals(at, ran.get());
    }

    @Test
    void syncTimerConsumer_canSelfCancel() {
        java.util.concurrent.atomic.AtomicInteger ran = new java.util.concurrent.atomic.AtomicInteger();
        scheduler.syncTimer(java.time.Duration.ZERO, java.time.Duration.ofMillis(50), task -> {
            if (ran.incrementAndGet() >= 2) {
                task.cancel();
            }
        });
        server.getScheduler().performTicks(6);
        org.junit.jupiter.api.Assertions.assertEquals(2, ran.get(),
                "self-cancel should stop the timer after the 2nd run");
    }

    @Test
    void entityTimerConsumer_selfCancels_onBukkitFallback() {
        org.bukkit.entity.Player player = server.addPlayer();
        java.util.concurrent.atomic.AtomicInteger ran = new java.util.concurrent.atomic.AtomicInteger();
        scheduler.entityTimer(player, java.time.Duration.ZERO, java.time.Duration.ofMillis(50), task -> {
            if (ran.incrementAndGet() >= 1) {
                task.cancel();
            }
        });
        server.getScheduler().performTicks(5);
        org.junit.jupiter.api.Assertions.assertEquals(1, ran.get());
    }
}
