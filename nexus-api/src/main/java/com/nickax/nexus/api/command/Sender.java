package com.nickax.nexus.api.command;

import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * A command sender, abstracted across platforms. Message it through {@link #audience()}.
 */
public interface Sender {

    /**
     * Returns the sender's display name.
     *
     * @return the sender's display name
     */
    @NotNull String name();

    /**
     * Returns the sender's UUID, or empty if the sender is the console.
     *
     * @return the sender's UUID, or empty for the console
     */
    @NotNull Optional<UUID> uuid();

    /**
     * Returns whether the sender has the given permission.
     *
     * @param permission the permission node
     * @return whether the sender has the permission
     */
    boolean hasPermission(@NotNull String permission);

    /**
     * Returns whether this sender is a player (as opposed to the console).
     *
     * @return {@code true} if the sender is a player
     */
    boolean isPlayer();

    /**
     * Returns this sender as an Adventure {@link Audience} for sending messages.
     *
     * @return the sender as an Adventure audience
     */
    @NotNull Audience audience();
}
