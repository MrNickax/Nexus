package com.nickax.nexus.api.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

/**
 * {@link TextFormatter} for legacy ampersand color codes ({@code &a}, {@code &l},
 * {@code &r}) with {@code &#RRGGBB} hex support enabled. Stateless singleton reached
 * through {@link TextFormatter#legacy()}.
 */
final class LegacyTextFormatter implements TextFormatter {

    static final LegacyTextFormatter INSTANCE = new LegacyTextFormatter();

    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    /**
     * Private constructor — use {@link TextFormatter#legacy()}.
     */
    private LegacyTextFormatter() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Component deserialize(@NotNull String input) {
        return serializer.deserialize(input);
    }
}
