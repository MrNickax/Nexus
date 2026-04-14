package com.nickax.nexus.common.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Legacy color engine that uses ampersand (&) based color codes.
 * This implementation supports traditional Minecraft color codes like &a, &c, &l, etc.
 * using the Adventure library's legacy serializer.
 */
public class LegacyMessageFormatter extends MessageFormatter {

    private final LegacyComponentSerializer legacyComponentSerializer = LegacyComponentSerializer.legacyAmpersand();

    /**
     * Converts a plain text input with legacy color codes into a colorized Adventure Component.
     * Color codes should be prefixed with an ampersand (&), for example: &aGreen text &cRed text
     *
     * @param input the plain text input containing legacy color codes
     * @return a Component with applied color formatting from the legacy codes
     */
    @Override
    public Component deserialize(String input) {
        return legacyComponentSerializer.deserialize(input);
    }
}