package com.nickax.nexus.bukkit.listener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry class that manages the registration and unregistration of SwitchableListener instances.
 */
public class ListenerRegistry {

    private final List<SwitchableListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Registers a new listener by enabling it and adding it to the registry
     *
     * @param listener The SwitchableListener to register
     */
    public void register(SwitchableListener listener) {
        listener.enable();
        listeners.add(listener);
    }

    /**
     * Unregisters a specific listener by disabling it and removing it from the registry
     *
     * @param listener The SwitchableListener to unregister
     */
    public void unregister(SwitchableListener listener) {
        listener.disable();
        listeners.remove(listener);
    }

    /**
     * Unregisters all listeners by disabling them and clearing the registry
     */
    public void unregisterAll() {
        listeners.forEach(SwitchableListener::disable);
        listeners.clear();
    }
}