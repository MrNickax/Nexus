package com.nickax.nexus.api.data;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * How a {@link DataStore} propagates writes from the cache to the backend.
 */
public sealed interface WritePolicy permits WritePolicy.WriteThrough, WritePolicy.WriteBehind {

    /**
     * Every save persists to the backend immediately.
     */
    record WriteThrough() implements WritePolicy {
    }

    /**
     * Saves mark the entry dirty; a background flush persists dirty entries every
     * {@code interval}. A final flush runs on store close.
     *
     * @param interval how often dirty entries are flushed
     */
    record WriteBehind(@NotNull Duration interval) implements WritePolicy {
    }

    /**
     * Returns a write-through policy: every save persists to the backend immediately.
     *
     * @return a write-through policy
     */
    static @NotNull WritePolicy writeThrough() {
        return new WriteThrough();
    }

    /**
     * Returns a write-behind policy: saves mark the entry dirty and a background
     * flush persists dirty entries on the given interval.
     *
     * @param interval how often to flush dirty entries
     * @return a write-behind policy with the given flush interval
     */
    static @NotNull WritePolicy writeBehind(@NotNull Duration interval) {
        return new WriteBehind(interval);
    }
}
