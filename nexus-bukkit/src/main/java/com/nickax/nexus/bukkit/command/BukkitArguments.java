package com.nickax.nexus.bukkit.command;

import com.nickax.nexus.api.command.ArgumentParseException;
import com.nickax.nexus.api.command.ArgumentType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Bukkit-specific {@link ArgumentType}s.
 */
public final class BukkitArguments {

    /**
     * Private constructor — this is a static-factory utility class.
     */
    private BukkitArguments() {
    }

    /**
     * Returns an argument type that resolves an online player by exact name and suggests
     * the names of all currently online players whose name starts with the partial input.
     *
     * @return the player argument type
     */
    public static @NotNull ArgumentType<Player> player() {
        return new ArgumentType<>() {

            /**
             * Resolves an online player by exact name.
             *
             * @param input the raw argument string
             * @return the online player
             * @throws ArgumentParseException if no online player has that exact name
             */
            @Override
            public @NotNull Player parse(@NotNull String input) throws ArgumentParseException {
                Player player = Bukkit.getPlayerExact(input);
                if (player == null) {
                    throw new ArgumentParseException("Player '" + input + "' is not online");
                }
                return player;
            }

            /**
             * Suggests online player names that start with the partial input (case-insensitive).
             *
             * @param partial the partial argument typed so far
             * @return matching online player names
             */
            @Override
            public @NotNull List<String> suggest(@NotNull String partial) {
                List<String> out = new ArrayList<>();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getName().toLowerCase().startsWith(partial.toLowerCase())) {
                        out.add(online.getName());
                    }
                }
                return out;
            }
        };
    }

    /**
     * Returns an argument type that resolves a loaded world by name and suggests the
     * names of all currently loaded worlds whose name starts with the partial input.
     *
     * @return the world argument type
     */
    public static @NotNull ArgumentType<World> world() {
        return new ArgumentType<>() {

            /**
             * Resolves a loaded world by name.
             *
             * @param input the raw argument string
             * @return the loaded world
             * @throws ArgumentParseException if no loaded world has that name
             */
            @Override
            public @NotNull World parse(@NotNull String input) throws ArgumentParseException {
                World world = Bukkit.getWorld(input);
                if (world == null) {
                    throw new ArgumentParseException("World '" + input + "' does not exist");
                }
                return world;
            }

            /**
             * Suggests loaded world names that start with the partial input (case-insensitive).
             *
             * @param partial the partial argument typed so far
             * @return matching loaded world names
             */
            @Override
            public @NotNull List<String> suggest(@NotNull String partial) {
                List<String> out = new ArrayList<>();
                for (World world : Bukkit.getWorlds()) {
                    if (world.getName().toLowerCase().startsWith(partial.toLowerCase())) {
                        out.add(world.getName());
                    }
                }
                return out;
            }
        };
    }
}
