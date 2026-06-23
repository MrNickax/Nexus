package com.nickax.nexus.bukkit.listener;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default {@link Listeners}: registers each listener against its owning plugin and tracks
 * it per owner so a single plugin's listeners can be unregistered without affecting
 * others. All operations are synchronized so they are safe from a shutdown hook running
 * off the main thread.
 */
public final class ListenersImpl implements Listeners {

    private final Map<Plugin, List<Listener>> registered = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void register(@NotNull Plugin owner, @NotNull Listener listener) {
        owner.getServer().getPluginManager().registerEvents(listener, owner);
        registered.computeIfAbsent(owner, key -> new ArrayList<>()).add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void unregister(@NotNull Plugin owner) {
        List<Listener> listeners = registered.remove(owner);
        if (listeners != null) {
            listeners.forEach(HandlerList::unregisterAll);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void unregisterAll() {
        registered.values().forEach(listeners -> listeners.forEach(HandlerList::unregisterAll));
        registered.clear();
    }
}
