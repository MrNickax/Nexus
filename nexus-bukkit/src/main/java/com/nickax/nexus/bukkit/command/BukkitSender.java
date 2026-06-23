package com.nickax.nexus.bukkit.command;

import com.nickax.nexus.api.command.Sender;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapts a Bukkit {@link CommandSender} to the agnostic {@link Sender}. Messaging
 * goes through the shared {@link BukkitAudiences} so it works on Spigot and Paper.
 */
public final class BukkitSender implements Sender {

    private final CommandSender sender;
    private final BukkitAudiences audiences;

    /**
     * Wraps a Bukkit sender for use as an agnostic {@link Sender}.
     *
     * @param sender    the Bukkit sender to wrap
     * @param audiences the shared audiences provider for Adventure messaging
     */
    public BukkitSender(@NotNull CommandSender sender, @NotNull BukkitAudiences audiences) {
        this.sender = sender;
        this.audiences = audiences;
    }

    /**
     * Returns the underlying Bukkit sender for callers that need Bukkit-specific APIs.
     *
     * @return the wrapped Bukkit sender
     */
    public @NotNull CommandSender bukkit() {
        return sender;
    }

    /**
     * Returns the display name of the sender as reported by Bukkit.
     *
     * @return the sender's name
     */
    @Override
    public @NotNull String name() {
        return sender.getName();
    }

    /**
     * Returns the UUID of this sender if it is a player, or an empty optional for the
     * console and other non-player senders.
     *
     * @return the player UUID, or empty
     */
    @Override
    public @NotNull Optional<UUID> uuid() {
        return sender instanceof Player player ? Optional.of(player.getUniqueId()) : Optional.empty();
    }

    /**
     * Returns whether the underlying sender holds the given permission node.
     *
     * @param permission the permission node to check
     * @return {@code true} if the sender has the permission
     */
    @Override
    public boolean hasPermission(@NotNull String permission) {
        return sender.hasPermission(permission);
    }

    /**
     * Returns whether the underlying sender is an in-game player.
     *
     * @return {@code true} if the sender is a {@link Player}
     */
    @Override
    public boolean isPlayer() {
        return sender instanceof Player;
    }

    /**
     * Returns an Adventure {@link Audience} for this sender, resolved through the
     * shared {@link BukkitAudiences} provider.
     *
     * @return the audience
     */
    @Override
    public @NotNull Audience audience() {
        return audiences.sender(sender);
    }
}
