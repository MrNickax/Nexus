package com.nickax.nexus.bukkit.schedule;

import com.nickax.nexus.api.schedule.ScheduledTask;
import org.jetbrains.annotations.NotNull;

/**
 * {@link ScheduledTask} backed by a Folia {@code ScheduledTask}. Loaded only on Folia.
 */
final class FoliaScheduledTask implements ScheduledTask {

    private final io.papermc.paper.threadedregions.scheduler.ScheduledTask task;

    /**
     * Wraps an existing Folia scheduled task.
     *
     * @param task the underlying Folia task
     */
    FoliaScheduledTask(@NotNull io.papermc.paper.threadedregions.scheduler.ScheduledTask task) {
        this.task = task;
    }

    /**
     * Cancels the underlying Folia task.
     */
    @Override
    public void cancel() {
        task.cancel();
    }

    /**
     * Returns whether the underlying Folia task has been cancelled.
     *
     * @return {@code true} if cancelled
     */
    @Override
    public boolean isCancelled() {
        return task.isCancelled();
    }
}
