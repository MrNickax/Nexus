package com.nickax.nexus.api.schedule;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Schedules tasks on the platform, abstracting Bukkit and Folia. {@code sync}
 * variants run on the main/region thread; {@code async} variants off it. Delays and
 * periods are {@link Duration}s, converted to ticks internally (minimum one tick;
 * a zero delay/period therefore runs on the next tick).
 *
 * <p>On Folia the {@code sync*} variants use the global region scheduler; tasks that
 * touch per-entity or per-chunk state must use the region/entity variants on
 * {@code BukkitScheduler} instead.
 */
public interface Scheduler {

    /**
     * Runs a task once on the main/region thread, on the next tick.
     *
     * @param task the task
     * @return a handle to cancel it
     */
    @NotNull ScheduledTask sync(@NotNull Runnable task);

    /**
     * Runs a task once off the main thread.
     *
     * @param task the task
     * @return a handle to cancel it
     */
    @NotNull ScheduledTask async(@NotNull Runnable task);

    /**
     * Runs a task once on the main/region thread after a delay.
     *
     * @param delay the delay
     * @param task  the task
     * @return a handle to cancel it
     */
    @NotNull ScheduledTask syncLater(@NotNull Duration delay, @NotNull Runnable task);

    /**
     * Runs a task once off the main thread after a delay.
     *
     * @param delay the delay
     * @param task  the task
     * @return a handle to cancel it
     */
    @NotNull ScheduledTask asyncLater(@NotNull Duration delay, @NotNull Runnable task);

    /**
     * Runs a task repeatedly on the main/region thread.
     *
     * @param delay  the delay before the first run (zero runs on the next tick)
     * @param period the period between runs
     * @param task   the task
     * @return a handle to cancel it
     */
    @NotNull ScheduledTask syncTimer(@NotNull Duration delay, @NotNull Duration period, @NotNull Runnable task);

    /**
     * Runs a task repeatedly off the main thread.
     *
     * @param delay  the delay before the first run (zero runs as soon as possible)
     * @param period the period between runs
     * @param task   the task
     * @return a handle to cancel it
     */
    @NotNull ScheduledTask asyncTimer(@NotNull Duration delay, @NotNull Duration period, @NotNull Runnable task);

    /**
     * Runs a task repeatedly on the main/region thread, passing the task its own
     * handle so it can cancel itself.
     *
     * @param delay  the delay before the first run (zero runs on the next tick)
     * @param period the period between runs
     * @param task   the task, receiving its {@link ScheduledTask} handle
     * @return the same handle
     */
    @NotNull ScheduledTask syncTimer(@NotNull Duration delay, @NotNull Duration period,
                                     @NotNull Consumer<ScheduledTask> task);

    /**
     * Runs a task repeatedly off the main thread, passing the task its own handle.
     *
     * @param delay  the delay before the first run
     * @param period the period between runs
     * @param task   the task, receiving its {@link ScheduledTask} handle
     * @return the same handle
     */
    @NotNull ScheduledTask asyncTimer(@NotNull Duration delay, @NotNull Duration period,
                                      @NotNull Consumer<ScheduledTask> task);
}
