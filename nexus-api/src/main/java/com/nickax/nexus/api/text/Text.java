package com.nickax.nexus.api.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Facade over MiniMessage for turning markup strings into Adventure
 * {@link Component}s. This is the canonical way to build text in Nexus; it is
 * platform-agnostic because it only produces components (sending them is the
 * platform's job, via an {@code Audience}).
 */
public final class Text {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Private constructor — utility class, not instantiable.
     */
    private Text() {
    }

    /**
     * Parses a single MiniMessage string into a component.
     *
     * @param markup the MiniMessage markup, e.g. {@code "<aqua>Hi"}
     * @return the parsed component
     */
    public static @NotNull Component of(@NotNull String markup) {
        return MINI_MESSAGE.deserialize(markup);
    }

    /**
     * Parses each string in a list into a component (e.g. for item lore).
     *
     * @param lines the MiniMessage lines
     * @return one component per line, in order
     */
    public static @NotNull List<Component> of(@NotNull List<String> lines) {
        return lines.stream().map(Text::of).toList();
    }

    /**
     * Parses each string into a component.
     *
     * @param lines the MiniMessage lines
     * @return one component per line, in order
     */
    public static @NotNull List<Component> of(@NotNull String @NotNull ... lines) {
        return Arrays.stream(lines).map(Text::of).toList();
    }
}
