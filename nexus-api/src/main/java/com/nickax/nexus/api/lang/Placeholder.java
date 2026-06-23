package com.nickax.nexus.api.lang;

import org.jetbrains.annotations.NotNull;

/**
 * A message placeholder. In a template, {@code {name}} is replaced with {@code value}.
 *
 * @param name  the placeholder name (without braces)
 * @param value the replacement value
 */
public record Placeholder(@NotNull String name, @NotNull String value) {

    /**
     * Creates a placeholder, coercing the value to a string.
     *
     * @param name  the placeholder name
     * @param value the replacement value (any object; {@code String.valueOf} is applied)
     * @return the placeholder
     */
    public static @NotNull Placeholder of(@NotNull String name, @NotNull Object value) {
        return new Placeholder(name, String.valueOf(value));
    }
}
