package com.nickax.nexus.bukkit.menu;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A clickable inventory item: an icon plus an optional click handler.
 */
public final class Button {

    private final ItemStack icon;
    private Consumer<ClickContext> handler;

    /**
     * Creates a button with the given icon and no click handler.
     *
     * @param icon the display item
     */
    private Button(@NotNull ItemStack icon) {
        this.icon = icon;
    }

    /**
     * Creates a button with the given icon and no click handler. Chain {@link #onClick}
     * to add behaviour.
     *
     * @param icon the display item
     * @return a new button
     */
    public static @NotNull Button of(@NotNull ItemStack icon) {
        return new Button(icon);
    }

    /**
     * A decorative, non-interactive button (e.g. a background pane).
     *
     * @param icon the display item
     * @return a no-op button
     */
    public static @NotNull Button filler(@NotNull ItemStack icon) {
        return new Button(icon);
    }

    /**
     * Sets the click handler.
     *
     * @param handler the handler
     * @return this
     */
    public @NotNull Button onClick(@NotNull Consumer<ClickContext> handler) {
        this.handler = handler;
        return this;
    }

    /**
     * Returns the icon displayed in the inventory slot for this button.
     *
     * @return the icon item stack
     */
    public @NotNull ItemStack icon() {
        return icon;
    }

    /**
     * Returns the click handler, or {@code null} if this button has no behaviour
     * (e.g. a filler pane).
     *
     * @return the handler, or {@code null} if none
     */
    public @Nullable Consumer<ClickContext> handler() {
        return handler;
    }
}
