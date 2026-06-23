package com.nickax.nexus.api;

import org.jetbrains.annotations.NotNull;

/**
 * Static access point for the active {@link Nexus} instance. The platform plugin
 * sets the instance on startup and clears it on shutdown; consumer plugins read it.
 */
public final class NexusProvider {

    private static volatile Nexus instance;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private NexusProvider() {
    }

    /**
     * Returns the active Nexus instance.
     *
     * @return the active instance, never {@code null}
     * @throws IllegalStateException if Nexus is not loaded yet
     */
    public static @NotNull Nexus get() {
        if (instance == null) {
            throw new IllegalStateException("Nexus is not loaded");
        }
        return instance;
    }

    /**
     * Sets the active instance. Called by the platform plugin only.
     *
     * @param nexus the instance to publish, or {@code null} to clear on shutdown
     */
    public static void set(Nexus nexus) {
        instance = nexus;
    }

    /**
     * Whether a Nexus instance is currently published.
     *
     * @return {@code true} if {@link #get()} would succeed
     */
    public static boolean isLoaded() {
        return instance != null;
    }
}
