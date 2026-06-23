package com.nickax.nexus.bukkit.schedule;

import com.nickax.nexus.api.schedule.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * {@link BukkitScheduler} over the classic {@code BukkitScheduler}. Works on Spigot
 * and Paper. Region- and entity-bound tasks run on the main thread (regions/entities
 * are not partitioned off Folia). Durations are converted to ticks (minimum one).
 */
public final class BukkitSchedulerImpl implements BukkitScheduler {

    private final Plugin plugin;
    private final org.bukkit.scheduler.BukkitScheduler bukkit;

    /**
     * Constructs a new BukkitSchedulerImpl backed by the server's classic scheduler.
     *
     * @param plugin the owning plugin
     */
    public BukkitSchedulerImpl(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.bukkit = plugin.getServer().getScheduler();
    }

    /**
     * Converts a duration to ticks, with a one-tick minimum.
     *
     * @param duration the duration
     * @return the tick count
     */
    static long toTicks(Duration duration) {
        return Math.max(1L, duration.toMillis() / 50L);
    }

    /**
     * Runs a task synchronously on the main thread.
     *
     * @param task the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask sync(@NotNull Runnable task) {
        return new BukkitScheduledTask(bukkit.runTask(plugin, task));
    }

    /**
     * Runs a task on an async thread pool thread.
     *
     * @param task the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask async(@NotNull Runnable task) {
        return new BukkitScheduledTask(bukkit.runTaskAsynchronously(plugin, task));
    }

    /**
     * Runs a task synchronously after a delay.
     *
     * @param delay the delay
     * @param task  the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask syncLater(@NotNull Duration delay, @NotNull Runnable task) {
        return new BukkitScheduledTask(bukkit.runTaskLater(plugin, task, toTicks(delay)));
    }

    /**
     * Runs a task asynchronously after a delay.
     *
     * @param delay the delay
     * @param task  the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask asyncLater(@NotNull Duration delay, @NotNull Runnable task) {
        return new BukkitScheduledTask(bukkit.runTaskLaterAsynchronously(plugin, task, toTicks(delay)));
    }

    /**
     * Runs a repeating task synchronously.
     *
     * @param delay  the initial delay
     * @param period the period between executions
     * @param task   the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask syncTimer(@NotNull Duration delay, @NotNull Duration period, @NotNull Runnable task) {
        return new BukkitScheduledTask(bukkit.runTaskTimer(plugin, task, toTicks(delay), toTicks(period)));
    }

    /**
     * Runs a repeating task asynchronously.
     *
     * @param delay  the initial delay
     * @param period the period between executions
     * @param task   the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask asyncTimer(@NotNull Duration delay, @NotNull Duration period, @NotNull Runnable task) {
        return new BukkitScheduledTask(bukkit.runTaskTimerAsynchronously(plugin, task, toTicks(delay), toTicks(period)));
    }

    /**
     * Runs a task on the main thread (regions are not partitioned on non-Folia servers).
     *
     * @param location the location (ignored in this implementation)
     * @param task     the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask region(@NotNull Location location, @NotNull Runnable task) {
        return sync(task);
    }

    /**
     * Runs a task on the main thread after a delay (regions are not partitioned on non-Folia servers).
     *
     * @param location the location (ignored in this implementation)
     * @param delay    the delay
     * @param task     the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask regionLater(@NotNull Location location, @NotNull Duration delay, @NotNull Runnable task) {
        return syncLater(delay, task);
    }

    /**
     * Runs a repeating task on the main thread (regions are not partitioned on non-Folia servers).
     *
     * @param location the location (ignored in this implementation)
     * @param delay    the initial delay
     * @param period   the period between executions
     * @param task     the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask regionTimer(@NotNull Location location, @NotNull Duration delay,
                                              @NotNull Duration period, @NotNull Runnable task) {
        return syncTimer(delay, period, task);
    }

    /**
     * Runs a task on the main thread (entity threads are not partitioned on non-Folia servers).
     *
     * @param entity the entity (ignored in this implementation)
     * @param task   the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask entity(@NotNull Entity entity, @NotNull Runnable task) {
        return sync(task);
    }

    /**
     * Runs a task on the main thread after a delay (entity threads are not partitioned on non-Folia servers).
     *
     * @param entity the entity (ignored in this implementation)
     * @param delay  the delay
     * @param task   the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask entityLater(@NotNull Entity entity, @NotNull Duration delay, @NotNull Runnable task) {
        return syncLater(delay, task);
    }

    /**
     * Runs a repeating task on the main thread (entity threads are not partitioned on non-Folia servers).
     *
     * @param entity the entity (ignored in this implementation)
     * @param delay  the initial delay
     * @param period the period between executions
     * @param task   the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask entityTimer(@NotNull Entity entity, @NotNull Duration delay,
                                              @NotNull Duration period, @NotNull Runnable task) {
        return syncTimer(delay, period, task);
    }

    /**
     * Runs a repeating task synchronously, passing the task its own handle so it can
     * cancel itself. The handle is stored in a one-element array before the first run
     * (always ≥1 tick later), so it is non-null when the task first executes.
     *
     * @param delay  the initial delay
     * @param period the period between executions
     * @param task   the task, receiving its {@link ScheduledTask} handle
     * @return the same handle
     */
    @Override
    public @NotNull ScheduledTask syncTimer(@NotNull Duration delay, @NotNull Duration period,
                                            @NotNull Consumer<ScheduledTask> task) {
        BukkitScheduledTask[] holder = new BukkitScheduledTask[1];
        org.bukkit.scheduler.BukkitTask bukkitTask =
                bukkit.runTaskTimer(plugin, () -> task.accept(holder[0]), toTicks(delay), toTicks(period));
        holder[0] = new BukkitScheduledTask(bukkitTask);
        return holder[0];
    }

    /**
     * Runs a repeating task asynchronously, passing the task its own handle so it can
     * cancel itself.
     *
     * @param delay  the initial delay
     * @param period the period between executions
     * @param task   the task, receiving its {@link ScheduledTask} handle
     * @return the same handle
     */
    @Override
    public @NotNull ScheduledTask asyncTimer(@NotNull Duration delay, @NotNull Duration period,
                                             @NotNull Consumer<ScheduledTask> task) {
        BukkitScheduledTask[] holder = new BukkitScheduledTask[1];
        org.bukkit.scheduler.BukkitTask bukkitTask =
                bukkit.runTaskTimerAsynchronously(plugin, () -> task.accept(holder[0]), toTicks(delay), toTicks(period));
        holder[0] = new BukkitScheduledTask(bukkitTask);
        return holder[0];
    }

    /**
     * Runs a repeating region-bound task on the main thread (regions are not
     * partitioned on non-Folia servers), passing the task its own handle.
     *
     * @param location the location (ignored in this implementation)
     * @param delay    the initial delay
     * @param period   the period between executions
     * @param task     the task, receiving its {@link ScheduledTask} handle
     * @return the same handle
     */
    @Override
    public @NotNull ScheduledTask regionTimer(@NotNull Location location, @NotNull Duration delay,
                                              @NotNull Duration period, @NotNull Consumer<ScheduledTask> task) {
        return syncTimer(delay, period, task);
    }

    /**
     * Runs a repeating entity-bound task on the main thread (entity threads are not
     * partitioned on non-Folia servers), passing the task its own handle.
     *
     * @param entity the entity (ignored in this implementation)
     * @param delay  the initial delay
     * @param period the period between executions
     * @param task   the task, receiving its {@link ScheduledTask} handle
     * @return the same handle
     */
    @Override
    public @NotNull ScheduledTask entityTimer(@NotNull Entity entity, @NotNull Duration delay,
                                              @NotNull Duration period, @NotNull Consumer<ScheduledTask> task) {
        return syncTimer(delay, period, task);
    }
}
