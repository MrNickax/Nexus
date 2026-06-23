package com.nickax.nexus.bukkit.menu;

import org.jetbrains.annotations.NotNull;

/**
 * A chest inventory slot. Rows and columns are 0-indexed (9 columns per row).
 *
 * @param index the raw slot index
 */
public record Slot(int index) {

    /**
     * Creates a slot from a row and column position (both 0-indexed, 9 columns per row).
     *
     * @param row    the 0-indexed row
     * @param column the 0-indexed column (0..8)
     * @return the slot at that position
     */
    public static @NotNull Slot of(int row, int column) {
        return new Slot(row * 9 + column);
    }

    /**
     * Creates a slot from a raw inventory slot index.
     *
     * @param index the raw slot index
     * @return the slot for the raw index
     */
    public static @NotNull Slot index(int index) {
        return new Slot(index);
    }
}
