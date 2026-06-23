package com.nickax.nexus.bukkit;

import com.nickax.nexus.api.NexusProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bootstraps Nexus on a Bukkit server: builds the hub from the plugin data
 * folder, publishes it on {@link NexusProvider}, and tears it down on disable.
 * Runs on Spigot, Paper and Folia.
 */
public final class NexusBukkitPlugin extends JavaPlugin {

    private BukkitNexusImpl nexus;

    /**
     * Builds the server-side Nexus hub, installs it in {@link NexusProvider}, and
     * logs the assigned node identifier.
     */
    @Override
    public void onEnable() {
        nexus = new BukkitNexusImpl(getDataFolder().toPath(), this);
        NexusProvider.set(nexus);
        getLogger().info("Nexus enabled (node-id=" + nexus.nodeId() + ")");
    }

    /**
     * Stops all managed services, shuts down the hub, and clears the
     * {@link NexusProvider} so dependent plugins do not hold a stale reference.
     */
    @Override
    public void onDisable() {
        if (nexus != null) {
            nexus.services().stopAll("nexus");
            nexus.shutdown();
        }
        NexusProvider.set(null);
        getLogger().info("Nexus disabled");
    }
}
