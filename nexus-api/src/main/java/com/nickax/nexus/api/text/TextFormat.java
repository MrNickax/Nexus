package com.nickax.nexus.api.text;

/**
 * The markup dialect a {@link TextFormatter} understands when turning configured
 * strings into Adventure components. Plugins typically expose this as a config option
 * so server owners can choose how they author messages.
 */
public enum TextFormat {

    /** Modern MiniMessage tags: gradients, hover/click, {@code <#RRGGBB>}, etc. */
    MINI_MESSAGE,

    /** Legacy ampersand codes ({@code &a}, {@code &l}, {@code &r}) plus {@code &#RRGGBB} hex. */
    LEGACY,

    /** Both: {@code &} codes (and {@code &#RRGGBB} hex) are converted to MiniMessage, then parsed as MiniMessage. */
    MIXED
}
