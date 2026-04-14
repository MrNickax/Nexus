package com.nickax.nexus.common.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.regex.Pattern;

/**
 * Mixed message formatter that supports both legacy ampersand (&) color codes and MiniMessage tags.
 * This implementation converts legacy color codes to MiniMessage format and then processes the result.
 * It supports:
 * - Hex colors with &#RRGGBB format (e.g., &#FF5733)
 * - Legacy color codes (&0-&f for colors, &k-&o for formatting, &r for reset)
 * - Native MiniMessage tags
 */
public class MixedMessageFormatter extends MessageFormatter {

    private final Pattern hexPattern = Pattern.compile("&#([a-fA-F0-9]{6})");
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Converts a plain text input with mixed legacy color codes and MiniMessage tags into a colorized Adventure Component.
     * Legacy color codes are first converted to MiniMessage format, then the entire string is deserialized.
     * The result is wrapped with <!italic> to disable automatic italic formatting.
     *
     * @param input the plain text input containing legacy color codes and/or MiniMessage tags
     * @return a Component with applied color formatting from both legacy codes and MiniMessage tags
     */
    @Override
    public Component deserialize(String input) {
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