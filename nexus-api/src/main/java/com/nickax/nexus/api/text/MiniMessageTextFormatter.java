package com.nickax.nexus.api.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

/**
 * {@link TextFormatter} that parses MiniMessage markup. Stateless singleton reached
 * through {@link TextFormatter#miniMessage()}.
 */
final class MiniMessageTextFormatter implements TextFormatter {

    static final MiniMessageTextFormatter INSTANCE = new MiniMessageTextFormatter();

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Private constructor — use {@link TextFormatter#miniMessage()}.
     */
    private MiniMessageTextFormatter() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Component deserialize(@NotNull String input) {
        return miniMessage.deserialize(input);
    }
}
