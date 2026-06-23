package com.nickax.nexus.bukkit.menu;

import com.nickax.nexus.api.text.Text;
import com.nickax.nexus.bukkit.text.LegacyText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A chest menu: a legacy-serialized title, a number of rows, and a slot→button map.
 * Build with {@link #chest(int, String)}.
 */
public final class Menu {

    private final String title;
    private final int rows;
    private final Map<Integer, Button> buttons;

    /**
     * Constructs a fully built menu. Only called from {@link Builder#build()}.
     *
     * @param title   the legacy-serialized title string
     * @param rows    the number of chest rows
     * @param buttons the immutable slot-to-button map
     */
    private Menu(@NotNull String title, int rows, @NotNull Map<Integer, Button> buttons) {
        this.title = title;
        this.rows = rows;
        this.buttons = buttons;
    }

    /**
     * @param rows             the number of rows (1..6)
     * @param miniMessageTitle the MiniMessage title
     * @return a new menu builder
     */
    public static @NotNull Builder chest(int rows, @NotNull String miniMessageTitle) {
        return new Builder(rows, miniMessageTitle);
    }

    /**
     * Returns the legacy-section-code title string used when creating the Bukkit inventory.
     *
     * @return the legacy-serialized title
     */
    public @NotNull String title() {
        return title;
    }

    /**
     * Returns the total number of slots in this menu ({@code rows * 9}).
     *
     * @return the inventory size
     */
    public int size() {
        return rows * 9;
    }

    /**
     * @param slot the raw slot index
     * @return the button at the slot, or {@code null}
     */
    public @Nullable Button buttonAt(int slot) {
        return buttons.get(slot);
    }

    /**
     * Returns an unmodifiable view of the slot-to-button map, used by {@link MenuHolder}
     * to populate the inventory on construction.
     *
     * @return the slot→button map
     */
    @NotNull Map<Integer, Button> buttons() {
        return java.util.Collections.unmodifiableMap(buttons);
    }

    /**
     * Fluent {@link Menu} builder.
     */
    public static final class Builder {

        private final int rows;
        private final String title;
        private final Map<Integer, Button> buttons = new HashMap<>();

        /**
         * Constructs a builder for a chest menu. Validates the row count and
         * pre-serializes the title from MiniMessage to legacy section codes.
         *
         * @param rows             the number of rows (1..6)
         * @param miniMessageTitle the MiniMessage title string
         * @throws IllegalArgumentException if {@code rows} is outside 1..6
         */
        private Builder(int rows, @NotNull String miniMessageTitle) {
            if (rows < 1 || rows > 6) {
                throw new IllegalArgumentException("rows must be 1..6 (got " + rows + ")");
            }
            this.rows = rows;
            this.title = LegacyText.serialize(Text.of(miniMessageTitle));
        }

        /**
         * Places a button at a slot.
         *
         * @param slot   the slot
         * @param button the button
         * @return this
         */
        public @NotNull Builder button(@NotNull Slot slot, @NotNull Button button) {
            buttons.put(slot.index(), button);
            return this;
        }

        /**
         * Fills every currently-empty slot with a button.
         *
         * @param button the filler button
         * @return this
         */
        public @NotNull Builder fill(@NotNull Button button) {
            for (int i = 0; i < rows * 9; i++) {
                buttons.putIfAbsent(i, button);
            }
            return this;
        }

        /**
         * @return the built menu
         */
        public @NotNull Menu build() {
            return new Menu(title, rows, new HashMap<>(buttons));
        }
    }
}
