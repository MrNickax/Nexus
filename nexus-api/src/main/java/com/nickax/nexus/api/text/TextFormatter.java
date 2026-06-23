package com.nickax.nexus.api.text;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Turns configured strings into Adventure {@link Component}s using a chosen markup
 * dialect ({@link TextFormat}). Where the {@link Text} facade is always MiniMessage,
 * a formatter lets a plugin honour a user-selected formatting mode read from its
 * config, so the same message author can use {@code &} codes or MiniMessage tags.
 * <p>
 * Implementations are stateless and thread-safe; obtain one with {@link #of(TextFormat)}
 * or the named factories.
 */
public interface TextFormatter {

    /**
     * Parses a single line of markup into a component.
     *
     * @param input the markup line
     * @return the parsed component
     */
    @NotNull Component deserialize(@NotNull String input);

    /**
     * Parses each line into its own component (e.g. for multi-line messages or lore).
     *
     * @param lines the markup lines
     * @return one component per line, in order
     */
    default @NotNull List<Component> deserialize(@NotNull List<String> lines) {
        return lines.stream().map(this::deserialize).toList();
    }

    /**
     * Returns the formatter for a markup dialect.
     *
     * @param format the markup dialect
     * @return the matching formatter
     */
    static @NotNull TextFormatter of(@NotNull TextFormat format) {
        return switch (format) {
            case MINI_MESSAGE -> miniMessage();
            case LEGACY -> legacy();
            case MIXED -> mixed();
        };
    }

    /**
     * Returns the MiniMessage formatter.
     *
     * @return the MiniMessage formatter
     */
    static @NotNull TextFormatter miniMessage() {
        return MiniMessageTextFormatter.INSTANCE;
    }

    /**
     * Returns the legacy ampersand-code formatter (also accepting {@code &#RRGGBB} hex).
     *
     * @return the legacy formatter
     */
    static @NotNull TextFormatter legacy() {
        return LegacyTextFormatter.INSTANCE;
    }

    /**
     * Returns the mixed formatter: legacy codes are rewritten to MiniMessage, then the
     * whole string is parsed as MiniMessage.
     *
     * @return the mixed formatter
     */
    static @NotNull TextFormatter mixed() {
        return MixedTextFormatter.INSTANCE;
    }
}
