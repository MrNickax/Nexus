package com.nickax.nexus.common.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * MiniMessage color engine that uses MiniMessage format for text formatting.
 * This implementation supports the MiniMessage tag-based format for colors, decorations,
 * and other text features using the Adventure library's MiniMessage serializer.
 */
public class MiniMessageMessageFormatter extends MessageFormatter {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Converts a plain text input with MiniMessage tags into a colorized Adventure Component.
     * Tags should use the MiniMessage format, for example: <green>Green text</green> <red>Red text</red>
     *
     * @param input the plain text input containing MiniMessage tags
     * @return a Component with applied formatting from the MiniMessage tags
     */
    @Override
    public Component deserialize(String input) {
        return miniMessage.deserialize(input);
    }
}