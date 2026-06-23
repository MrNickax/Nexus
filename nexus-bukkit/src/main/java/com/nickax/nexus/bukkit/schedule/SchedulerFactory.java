package com.nickax.nexus.bukkit.schedule;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Picks the right {@link BukkitScheduler} for the running platform. Folia is detected
 * once; {@link FoliaSchedulerImpl} is only referenced on Folia, so Spigot never
 * loads it.
 */
public final class SchedulerFactory {

    private static final boolean FOLIA = detectFolia();

    /**
     * Private constructor — this is a static-factory utility class.
     */
    private SchedulerFactory() {
    }

    /**
     * Returns {@code true} if the server is running Folia, detected by the presence
     * of {@code io.papermc.paper.threadedregions.RegionizedServer} on the classpath.
     *
     * @return {@code true} on Folia, {@code false} on Spigot/Paper
     */
    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Creates the appropriate {@link BukkitScheduler} for the running server.
     *
     * @param plugin the owning plugin
     * @return a {@link FoliaSchedulerImpl} on Folia, or a {@link BukkitSchedulerImpl} otherwise
     */
    public static @NotNull BukkitScheduler create(@NotNull Plugin plugin) {
        return FOLIA ? new FoliaSchedulerImpl(plugin) : new BukkitSchedulerImpl(plugin);
    }
}
