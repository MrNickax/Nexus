package com.nickax.nexus.bukkit.schedule;

import com.nickax.nexus.api.schedule.ScheduledTask;
import com.nickax.nexus.api.schedule.Scheduler;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Server-side scheduler. Adds region- and entity-bound scheduling to the agnostic
 * {@link Scheduler}. On Folia these run on the owning region's / entity's thread; on
 * Spigot and Paper they fall back to the main thread.
 */
public interface BukkitScheduler extends Scheduler {

    /**
     * Runs a task on the region owning a location.
     *
     * @param location the location whose region owns the task
     * @param task     the task
     * @return a handle to cancel it
     */
    @NotNull ScheduledTask region(@NotNull Location location, @NotNull Runnable task);

    /**
     * Runs a task on a location's region after a delay.
     *
     * @param location the location
     * @param delay    the delay
     * @param task     the task
     * @return a handle to cancel it
     */
    @NotNull ScheduledTask regionLater(@NotNull Location location, @NotNull Duration delay, @NotNull Runnable task);

    /**
     * Runs a repeating task on a location's region.
     *
     * @param location the location
     * @param delay    the initial delay
     * @param period   the period
     * @param task     the task
     * @return a handle to cancel it
     */
    @NotNull ScheduledTask regionTimer(@NotNull Location location, @NotNull Duration delay,
                                       @NotNull Duration period, @NotNull Runnable task);

    /**
     * Runs a task on the thread owning an entity.
     *
     * @param entity the entity
     * @param task   the task
     * @return a handle to cancel it
     */
    @NotNull ScheduledTask entity(@NotNull Entity entity, @NotNull Runnable task);

    /**
     * Runs a task on an entity's thread after a delay.
     *
     * @param entity the entity
     * @param delay  the delay
     * @param task   the task
     * @return a handle to cancel it
     */
    @NotNull ScheduledTask entityLater(@NotNull Entity entity, @NotNull Duration delay, @NotNull Runnable task);

    /**
     * Runs a repeating task on an entity's thread.
     *
     * @param entity the entity
     * @param delay  the initial delay
     * @param period the period
     * @param task   the task
     * @return a handle to cancel it
     */
    @NotNull ScheduledTask entityTimer(@NotNull Entity entity, @NotNull Duration delay,
                                       @NotNull Duration period, @NotNull Runnable task);

    /**
     * Repeating region-bound task that receives its own handle for self-cancellation.
     *
     * @param location the location
     * @param delay    the initial delay
     * @param period   the period
     * @param task     the task, receiving its {@link ScheduledTask} handle
     * @return the same handle
     */
    @NotNull ScheduledTask regionTimer(@NotNull Location location, @NotNull Duration delay,
                                       @NotNull Duration period, @NotNull Consumer<ScheduledTask> task);

    /**
     * Repeating entity-bound task that receives its own handle for self-cancellation.
     *
     * @param entity the entity
     * @param delay  the initial delay
     * @param period the period
     * @param task   the task, receiving its {@link ScheduledTask} handle
     * @return the same handle
     */
    @NotNull ScheduledTask entityTimer(@NotNull Entity entity, @NotNull Duration delay,
                                       @NotNull Duration period, @NotNull Consumer<ScheduledTask> task);
}
