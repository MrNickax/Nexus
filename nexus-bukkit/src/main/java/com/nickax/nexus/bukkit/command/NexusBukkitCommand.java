package com.nickax.nexus.bukkit.command;

import com.nickax.nexus.api.command.Command;
import com.nickax.nexus.api.command.CommandFeedback;
import com.nickax.nexus.api.command.CommandResult;
import com.nickax.nexus.api.text.Text;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * A Bukkit command that delegates to a Nexus {@link Command}. Builds a
 * {@link BukkitSender}, dispatches, and reports basic failures. Remembers its owning
 * plugin so the registry can unregister a single plugin's commands.
 */
final class NexusBukkitCommand extends org.bukkit.command.Command {

    private final Plugin owner;
    private final Command command;
    private final BukkitAudiences audiences;
    private final CommandFeedback feedback;

    /**
     * Constructs the Bukkit command wrapper. The name, description, usage, and aliases
     * are pulled from the Nexus command definition.
     *
     * @param owner     the plugin that registered the command
     * @param command   the Nexus command to delegate to
     * @param audiences the shared audiences provider
     * @param feedback  the feedback used to build messages for non-success results
     */
    NexusBukkitCommand(@NotNull Plugin owner, @NotNull Command command, @NotNull BukkitAudiences audiences, @NotNull CommandFeedback feedback) {
        super(command.name(), command.description() == null ? "" : command.description(), "/" + command.name(), command.aliases());
        this.owner = owner;
        this.command = command;
        this.audiences = audiences;
        this.feedback = feedback;
    }

    /**
     * Returns whether this command is owned by the given plugin, so the registry can
     * unregister only the commands a specific plugin registered.
     *
     * @param owner the plugin to compare against
     * @return {@code true} if this command is owned by {@code owner}
     */
    boolean ownedBy(@NotNull Plugin owner) {
        return this.owner == owner;
    }

    /**
     * Dispatches the command to the wrapped Nexus command. On any non-success result,
     * asks the {@link CommandFeedback} for a message and sends it, unless the feedback
     * returns {@code null}.
     *
     * @param sender the Bukkit command sender
     * @param label  the alias used (unused; the Nexus command resolves its own label)
     * @param args   the raw argument tokens
     * @return always {@code true} to suppress Bukkit's built-in usage message
     */
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String @NonNull [] args) {
        BukkitSender nexusSender = new BukkitSender(sender, audiences);

        CommandResult result = command.dispatch(nexusSender, args);
        if (result != CommandResult.SUCCESS) {
            String markup = feedback.message(nexusSender, result);
            if (markup != null) {
                nexusSender.audience().sendMessage(Text.of(markup));
            }
        }

        return true;
    }

    /**
     * Delegates tab-completion to the wrapped Nexus command.
     *
     * @param sender the Bukkit command sender
     * @param alias  the alias used (passed through to the Nexus command)
     * @param args   the current argument tokens including the partial last token
     * @return the list of tab-completion suggestions
     */
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String @NonNull [] args) {
        return command.complete(new BukkitSender(sender, audiences), args);
    }
}
