package com.nickax.nexus.bukkit.schedule;

import com.nickax.nexus.api.schedule.ScheduledTask;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/**
 * {@link ScheduledTask} backed by a {@link BukkitTask}.
 */
final class BukkitScheduledTask implements ScheduledTask {

    private final BukkitTask task;

    /**
     * Wraps an existing Bukkit task.
     *
     * @param task the underlying Bukkit task
     */
    BukkitScheduledTask(@NotNull BukkitTask task) {
        this.task = task;
    }

    /**
     * Cancels the underlying Bukkit task.
     */
    @Override
    public void cancel() {
        task.cancel();
    }

    /**
     * Returns whether the underlying Bukkit task has been cancelled.
     *
     * @return {@code true} if cancelled
     */
    @Override
    public boolean isCancelled() {
        return task.isCancelled();
    }
}
