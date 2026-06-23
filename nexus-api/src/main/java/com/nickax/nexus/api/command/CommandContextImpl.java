package com.nickax.nexus.api.command;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Default {@link CommandContext}.
 */
final class CommandContextImpl implements CommandContext {

    private final Sender sender;
    private final String[] raw;
    private final Map<String, Object> parsed;

    /**
     * Constructs a new CommandContextImpl.
     *
     * @param sender the sender who executed the command
     * @param raw    the raw argument tokens passed to the executed node
     * @param parsed the map of parsed argument values keyed by argument name
     */
    CommandContextImpl(@NotNull Sender sender, @NotNull String[] raw, @NotNull Map<String, Object> parsed) {
        this.sender = sender;
        this.raw = raw;
        this.parsed = parsed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Sender sender() {
        return sender;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull String[] raw() {
        return raw.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> @NotNull T get(@NotNull String name) {
        if (!parsed.containsKey(name)) {
            throw new IllegalArgumentException("No parsed argument named '" + name + "'");
        }
        return (T) parsed.get(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has(@NotNull String name) {
        return parsed.containsKey(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt(@NotNull String name) {
        return this.<Integer>get(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull String getString(@NotNull String name) {
        return this.get(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reply(@NotNull Component message) {
        sender.audience().sendMessage(message);
    }
}
