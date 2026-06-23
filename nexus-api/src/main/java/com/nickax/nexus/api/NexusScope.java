package com.nickax.nexus.api;

import com.nickax.nexus.api.data.DataService;
import com.nickax.nexus.api.service.Service;
import org.jetbrains.annotations.NotNull;

/**
 * A per-plugin view of Nexus. Obtain one with {@link Nexus#scope(String)} (or
 * {@code BukkitNexus.scope(plugin)}). Data stores created through {@link #data()}
 * are automatically namespaced with the scope id, so two plugins cannot collide;
 * services registered with {@link #register(Service)} are all stopped on
 * {@link #close()} (call it in your plugin's shutdown).
 *
 * <p>Global, inherently per-plugin subsystems (lang, messaging, scheduler,
 * configs, locks, webhooks) are used directly from the {@link Nexus} hub.
 */
public interface NexusScope extends AutoCloseable {

    /**
     * Returns the id this scope was created with.
     *
     * @return the scope id (e.g. the plugin name)
     */
    @NotNull String id();

    /**
     * The scoped data factory: store names are automatically prefixed with the
     * scope id, so {@code store("accounts")} maps to the global store
     * {@code <id>_accounts}.
     * Note the global store name is {@code <id>_<name>}; keep store names from overlapping another scope's id prefix.
     *
     * @return the scoped data service
     */
    @NotNull DataService data();

    /**
     * Registers and starts a service owned by this scope; it is stopped on
     * {@link #close()}.
     *
     * @param service the service
     * @param <T>     the service type
     * @return the started service
     */
    <T extends Service> @NotNull T register(@NotNull T service);

    /**
     * Stops every service registered through this scope (in reverse order). Call
     * from the owning plugin's shutdown.
     * This implementation does not throw a checked exception.
     */
    @Override
    void close();
}
