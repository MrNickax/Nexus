package com.nickax.nexus.bukkit.schedule;

import com.nickax.nexus.api.schedule.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * {@link BukkitScheduler} over Folia's global region, async, region, and entity
 * schedulers. This class is only instantiated on Folia (see {@link SchedulerFactory});
 * it is never loaded on Spigot, so its Folia-API references cause no linkage error
 * there.
 */
public final class FoliaSchedulerImpl implements BukkitScheduler {

    private final Plugin plugin;

    /**
     * Constructs a new FoliaSchedulerImpl for the given plugin.
     *
     * @param plugin the owning plugin
     */
    public FoliaSchedulerImpl(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Runs a task on the global region scheduler (equivalent to the main thread on non-Folia servers).
     *
     * @param task the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask sync(@NotNull Runnable task) {
        return new FoliaScheduledTask(Bukkit.getGlobalRegionScheduler().run(plugin, scheduled -> task.run()));
    }

    /**
     * Runs a task immediately on the async scheduler.
     *
     * @param task the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask async(@NotNull Runnable task) {
        return new FoliaScheduledTask(Bukkit.getAsyncScheduler().runNow(plugin, scheduled -> task.run()));
    }

    /**
     * Runs a task on the global region scheduler after a tick-converted delay.
     *
     * @param delay the delay
     * @param task  the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask syncLater(@NotNull Duration delay, @NotNull Runnable task) {
        return new FoliaScheduledTask(Bukkit.getGlobalRegionScheduler()
                .runDelayed(plugin, scheduled -> task.run(), BukkitSchedulerImpl.toTicks(delay)));
    }

    /**
     * Runs a task on the async scheduler after a millisecond delay.
     *
     * @param delay the delay
     * @param task  the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask asyncLater(@NotNull Duration delay, @NotNull Runnable task) {
        return new FoliaScheduledTask(Bukkit.getAsyncScheduler()
                .runDelayed(plugin, scheduled -> task.run(), toMillis(delay), TimeUnit.MILLISECONDS));
    }

    /**
     * Runs a repeating task on the global region scheduler.
     *
     * @param delay  the initial delay
     * @param period the period between executions
     * @param task   the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask syncTimer(@NotNull Duration delay, @NotNull Duration period, @NotNull Runnable task) {
        return new FoliaScheduledTask(Bukkit.getGlobalRegionScheduler()
                .runAtFixedRate(plugin, scheduled -> task.run(),
                        BukkitSchedulerImpl.toTicks(delay), BukkitSchedulerImpl.toTicks(period)));
    }

    /**
     * Runs a repeating task on the async scheduler with millisecond intervals.
     *
     * @param delay  the initial delay
     * @param period the period between executions
     * @param task   the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask asyncTimer(@NotNull Duration delay, @NotNull Duration period, @NotNull Runnable task) {
        return new FoliaScheduledTask(Bukkit.getAsyncScheduler()
                .runAtFixedRate(plugin, scheduled -> task.run(),
                        toMillis(delay), toMillis(period), TimeUnit.MILLISECONDS));
    }

    /**
     * Runs a task on the region scheduler for the given location.
     *
     * @param location the location whose owning region runs the task
     * @param task     the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask region(@NotNull Location location, @NotNull Runnable task) {
        return new FoliaScheduledTask(Bukkit.getRegionScheduler().run(plugin, location, scheduled -> task.run()));
    }

    /**
     * Runs a task on the region scheduler for the given location after a delay.
     *
     * @param location the location whose owning region runs the task
     * @param delay    the delay
     * @param task     the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask regionLater(@NotNull Location location, @NotNull Duration delay, @NotNull Runnable task) {
        return new FoliaScheduledTask(
                Bukkit.getRegionScheduler().runDelayed(plugin, location, scheduled -> task.run(), BukkitSchedulerImpl.toTicks(delay))
        );
    }

    /**
     * Runs a repeating task on the region scheduler for the given location.
     *
     * @param location the location whose owning region runs the task
     * @param delay    the initial delay
     * @param period   the period between executions
     * @param task     the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask regionTimer(@NotNull Location location, @NotNull Duration delay,
                                              @NotNull Duration period, @NotNull Runnable task) {
        return new FoliaScheduledTask(
                Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, scheduled -> task.run(), BukkitSchedulerImpl.toTicks(delay), BukkitSchedulerImpl.toTicks(period))
        );
    }

    /**
     * Runs a task on the entity's owning thread. Returns a pre-cancelled task if the
     * entity has already been retired.
     *
     * @param entity the entity
     * @param task   the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask entity(@NotNull Entity entity, @NotNull Runnable task) {
        @Nullable io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask =
                entity.getScheduler().run(plugin, scheduled -> task.run(), null);
        return foliaTask != null ? new FoliaScheduledTask(foliaTask) : cancelledTask();
    }

    /**
     * Runs a task on the entity's owning thread after a delay. Returns a pre-cancelled
     * task if the entity has already been retired.
     *
     * @param entity the entity
     * @param delay  the delay
     * @param task   the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask entityLater(@NotNull Entity entity, @NotNull Duration delay, @NotNull Runnable task) {
        @Nullable io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask =
                entity.getScheduler().runDelayed(plugin, scheduled -> task.run(), null,
                        BukkitSchedulerImpl.toTicks(delay));
        return foliaTask != null ? new FoliaScheduledTask(foliaTask) : cancelledTask();
    }

    /**
     * Runs a repeating task on the entity's owning thread. Returns a pre-cancelled task
     * if the entity has already been retired.
     *
     * @param entity the entity
     * @param delay  the initial delay
     * @param period the period between executions
     * @param task   the task
     * @return a handle to cancel it
     */
    @Override
    public @NotNull ScheduledTask entityTimer(@NotNull Entity entity, @NotNull Duration delay,
                                              @NotNull Duration period, @NotNull Runnable task) {
        @Nullable io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask =
                entity.getScheduler().runAtFixedRate(plugin, scheduled -> task.run(), null,
                        BukkitSchedulerImpl.toTicks(delay), BukkitSchedulerImpl.toTicks(period));
        return foliaTask != null ? new FoliaScheduledTask(foliaTask) : cancelledTask();
    }

    /**
     * Runs a repeating task on the global region scheduler, passing the task its own
     * handle so it can cancel itself.
     *
     * @param delay  the initial delay
     * @param period the period between executions
     * @param task   the task, receiving its {@link ScheduledTask} handle
     * @return the same handle
     */
    @Override
    public @NotNull ScheduledTask syncTimer(@NotNull Duration delay, @NotNull Duration period,
                                            @NotNull Consumer<ScheduledTask> task) {
        return new FoliaScheduledTask(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin,
                foliaTask -> task.accept(new FoliaScheduledTask(foliaTask)),
                BukkitSchedulerImpl.toTicks(delay), BukkitSchedulerImpl.toTicks(period)));
    }

    /**
     * Runs a repeating task on the async scheduler with millisecond intervals, passing
     * the task its own handle so it can cancel itself.
     *
     * @param delay  the initial delay
     * @param period the period between executions
     * @param task   the task, receiving its {@link ScheduledTask} handle
     * @return the same handle
     */
    @Override
    public @NotNull ScheduledTask asyncTimer(@NotNull Duration delay, @NotNull Duration period,
                                             @NotNull Consumer<ScheduledTask> task) {
        return new FoliaScheduledTask(Bukkit.getAsyncScheduler().runAtFixedRate(plugin,
                foliaTask -> task.accept(new FoliaScheduledTask(foliaTask)),
                toMillis(delay), toMillis(period), TimeUnit.MILLISECONDS));
    }

    /**
     * Runs a repeating task on the region scheduler for the given location, passing the
     * task its own handle so it can cancel itself.
     *
     * @param location the location whose owning region runs the task
     * @param delay    the initial delay
     * @param period   the period between executions
     * @param task     the task, receiving its {@link ScheduledTask} handle
     * @return the same handle
     */
    @Override
    public @NotNull ScheduledTask regionTimer(@NotNull Location location, @NotNull Duration delay,
                                              @NotNull Duration period, @NotNull Consumer<ScheduledTask> task) {
        return new FoliaScheduledTask(Bukkit.getRegionScheduler().runAtFixedRate(plugin, location,
                foliaTask -> task.accept(new FoliaScheduledTask(foliaTask)),
                BukkitSchedulerImpl.toTicks(delay), BukkitSchedulerImpl.toTicks(period)));
    }

    /**
     * Runs a repeating task on the entity's owning thread, passing the task its own
     * handle so it can cancel itself. Returns a pre-cancelled task if the entity has
     * already been retired.
     *
     * @param entity the entity
     * @param delay  the initial delay
     * @param period the period between executions
     * @param task   the task, receiving its {@link ScheduledTask} handle
     * @return the same handle, or a pre-cancelled task if the entity was retired
     */
    @Override
    public @NotNull ScheduledTask entityTimer(@NotNull Entity entity, @NotNull Duration delay,
                                              @NotNull Duration period, @NotNull Consumer<ScheduledTask> task) {
        @Nullable io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask =
                entity.getScheduler().runAtFixedRate(plugin,
                        ft -> task.accept(new FoliaScheduledTask(ft)), null,
                        BukkitSchedulerImpl.toTicks(delay), BukkitSchedulerImpl.toTicks(period));
        return foliaTask != null ? new FoliaScheduledTask(foliaTask) : cancelledTask();
    }

    /**
     * Converts a duration to milliseconds with a one-millisecond floor, so Folia's
     * async runDelayed/runAtFixedRate never receive a zero delay.
     *
     * @param duration the duration
     * @return the milliseconds, at least 1
     */
    private static long toMillis(Duration duration) {
        return Math.max(1L, duration.toMillis());
    }

    /**
     * Returns a no-op, pre-cancelled {@link ScheduledTask} for retired entities.
     *
     * @return an already-cancelled task
     */
    private static ScheduledTask cancelledTask() {
        return new ScheduledTask() {

            /**
             * No-op: the task was never scheduled because the entity had already been retired.
             */
            @Override
            public void cancel() {
            }

            /**
             * Always returns {@code true} because this task was never actually scheduled.
             *
             * @return {@code true}
             */
            @Override
            public boolean isCancelled() {
                return true;
            }
        };
    }
}
