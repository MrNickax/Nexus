package com.nickax.nexus.bukkit.command;

import com.nickax.nexus.api.command.Command;
import com.nickax.nexus.api.command.CommandFeedback;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Default {@link Commands}: wraps each Nexus command in a {@link NexusBukkitCommand}
 * and registers it into the server command map under the {@code nexus} fallback prefix.
 */
public final class BukkitCommands implements Commands {

    private final BukkitAudiences audiences;
    private final CommandMap commandMap;
    private final List<NexusBukkitCommand> registered = new ArrayList<>();

    /**
     * Constructs the command registry, resolving the server's command map from the plugin.
     *
     * @param plugin    the owning plugin
     * @param audiences the shared audiences provider, used when wrapping senders
     */
    public BukkitCommands(@NotNull Plugin plugin, @NotNull BukkitAudiences audiences) {
        this.audiences = audiences;
        this.commandMap = plugin.getServer().getCommandMap();
    }

    /**
     * Registers the command owned by {@code owner} with the
     * {@linkplain CommandFeedback#defaults() default} failure messages.
     *
     * @param owner   the plugin that owns the command
     * @param command the command to register
     */
    @Override
    public void register(@NotNull Plugin owner, @NotNull Command command) {
        register(owner, command, CommandFeedback.defaults());
    }

    /**
     * Wraps the command in a {@link NexusBukkitCommand} and registers it into the
     * server command map under the {@code nexus} fallback prefix. Commands that fail to
     * register (duplicate name) are silently skipped.
     *
     * @param owner    the plugin that owns the command
     * @param command  the command to register
     * @param feedback the feedback used for non-success results
     */
    @Override
    public void register(@NotNull Plugin owner, @NotNull Command command, @NotNull CommandFeedback feedback) {
        NexusBukkitCommand bukkit = new NexusBukkitCommand(owner, command, audiences, feedback);
        if (commandMap.register("nexus", bukkit)) {
            registered.add(bukkit);
        }
    }

    /**
     * Unregisters every command owned by {@code owner} from the command map and evicts
     * them from the known-commands table, leaving every other plugin's commands untouched.
     *
     * @param owner the plugin whose commands should be unregistered
     */
    @Override
    public void unregister(@NotNull Plugin owner) {
        List<NexusBukkitCommand> toRemove = new ArrayList<>();
        for (NexusBukkitCommand bukkit : registered) {
            if (bukkit.ownedBy(owner)) {
                bukkit.unregister(commandMap);
                toRemove.add(bukkit);
            }
        }
        registered.removeAll(toRemove);
        evictFromKnownCommands(toRemove);
    }

    /**
     * Unregisters every command registered through this registry from the command map
     * and evicts them from the known-commands lookup table so a reload does not leave
     * stale entries behind.
     */
    @Override
    public void unregisterAll() {
        for (NexusBukkitCommand command : registered) {
            command.unregister(commandMap);
        }
        evictFromKnownCommands(registered);
        registered.clear();
    }

    /**
     * Reflectively removes the given commands from the command map's known-commands map.
     * This is necessary because {@link org.bukkit.command.Command#unregister} only marks
     * the command as unregistered but does not remove it from the map on all server
     * implementations. Silently ignores servers where {@code getKnownCommands} is not
     * accessible.
     *
     * @param toEvict the wrappers to remove from the known-commands map
     */
    @SuppressWarnings("unchecked")
    private void evictFromKnownCommands(Collection<NexusBukkitCommand> toEvict) {
        if (toEvict.isEmpty()) {
            return;
        }
        try {
            Method method = commandMap.getClass().getMethod("getKnownCommands");
            // The map values are Bukkit commands (org.bukkit.command.Command), not Nexus
            // commands; typing it as such keeps removeIf's predicate from casting every
            // entry to the Nexus Command type (which would fail on the server defaults).
            Map<String, org.bukkit.command.Command> known =
                    (Map<String, org.bukkit.command.Command>) method.invoke(commandMap);
            known.values().removeIf(toEvict::contains);
        } catch (ReflectiveOperationException ignored) {
            // getKnownCommands not available on this command map; Command#unregister already ran
        }
    }
}
