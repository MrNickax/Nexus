package com.nickax.nexus.bukkit.listener;

import com.nickax.nexus.common.misc.Switchable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A base listener class that can be enabled and disabled at runtime.
 * Implements both Bukkit's {@link Listener} interface and {@link Switchable} interface
 * to provide event handling capabilities that can be dynamically registered and unregistered.
 *
 * <p>This class provides the basic functionality to:
 * <ul>
 *     <li>Register event handlers when enabled</li>
 *     <li>Unregister event handlers when disabled</li>
 * </ul>
 *
 * @see Listener
 * @see Switchable
 */
public class SwitchableListener implements Listener, Switchable {

    protected final JavaPlugin plugin;

    /**
     * Creates a new SwitchableListener
     *
     * @param plugin The JavaPlugin instance this listener will be registered to
     */
    public SwitchableListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Enables this listener by registering its event handlers.
     */
    @Override
    public void enable() {
        SwitchableListener listener = this;
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    /**
     * Disables this listener by unregistering all its event handlers.
     */
    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }
}