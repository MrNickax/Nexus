package com.nickax.nexus.api.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * {@link TextFormatter} that accepts both legacy ampersand codes and MiniMessage tags.
 * Legacy {@code &#RRGGBB} hex and {@code &0}–{@code &f}/{@code &k}–{@code &r} codes are
 * rewritten to their MiniMessage equivalents, then the whole string is parsed as
 * MiniMessage. The result is prefixed with {@code <!italic>} so legacy text does not
 * inherit MiniMessage's default italic on some contexts. Stateless singleton reached
 * through {@link TextFormatter#mixed()}.
 */
final class MixedTextFormatter implements TextFormatter {

    static final MixedTextFormatter INSTANCE = new MixedTextFormatter();

    private final Pattern hexPattern = Pattern.compile("&#([a-fA-F0-9]{6})");
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Private constructor — use {@link TextFormatter#mixed()}.
     */
    private MixedTextFormatter() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Component deserialize(@NotNull String input) {
        String result = hexPattern.matcher(input).replaceAll("<#$1>");

        result = result
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&k", "<obfuscated>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underline>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");

        return miniMessage.deserialize("<!italic>" + result);
    }
}
