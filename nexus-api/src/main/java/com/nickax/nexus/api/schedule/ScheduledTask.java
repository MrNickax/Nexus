package com.nickax.nexus.api.schedule;

/**
 * A handle to a scheduled (delayed or repeating) task.
 */
public interface ScheduledTask {

    /**
     * Cancels the task. Safe to call more than once.
     */
    void cancel();

    /**
     * Returns whether this task has been cancelled.
     *
     * @return {@code true} if the task has been cancelled
     */
    boolean isCancelled();
}
