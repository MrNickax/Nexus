package com.nickax.nexus.bukkit.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

/**
 * Serializes Adventure components to legacy section-code strings (hex-aware,
 * {@code §x}) for Bukkit APIs that lack a cross-platform Component setter
 * (item meta, inventory titles). Works on Spigot and Paper.
 */
public final class LegacyText {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .build();

    /**
     * Private constructor — this is a static-utility class.
     */
    private LegacyText() {
    }

    /**
     * Serializes an Adventure component to a legacy section-code string with hex colour support.
     *
     * @param component the component to serialize
     * @return the legacy section-code string
     */
    public static @NotNull String serialize(@NotNull Component component) {
        return SERIALIZER.serialize(component);
    }
}
