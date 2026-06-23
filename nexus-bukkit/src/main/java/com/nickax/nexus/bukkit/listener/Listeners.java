package com.nickax.nexus.bukkit.listener;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Registers Bukkit listeners on behalf of a plugin and unregisters them by owner, so a
 * plugin can clean up exactly its own listeners on disable (PlugMan-style unload/reload
 * safe) without touching any other plugin's.
 */
public interface Listeners {

    /**
     * Registers a listener owned by {@code owner}. The listener is registered against the
     * owning plugin, so Bukkit attributes it to that plugin.
     *
     * @param owner    the plugin that owns the listener
     * @param listener the listener
     */
    void register(@NotNull Plugin owner, @NotNull Listener listener);

    /**
     * Unregisters every listener registered through this registry for {@code owner},
     * leaving other plugins' listeners untouched.
     *
     * @param owner the plugin whose listeners should be unregistered
     */
    void unregister(@NotNull Plugin owner);

    /**
     * Unregisters every listener registered through this registry, for all owners.
     */
    void unregisterAll();
}
