package com.nickax.nexus.api.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * A view over a nested configuration tree. Paths are dotted (e.g.
 * {@code "limits.max"}). Missing typed values return a Bukkit-like neutral default
 * ({@code null} / {@code 0} / {@code false} / empty list) unless an explicit
 * default is supplied. Keys must not contain a literal '.' — dots are reserved as the path separator.
 */
public interface ConfigSection {

    /**
     * Returns whether a value exists at the given path.
     *
     * @param path dotted path
     * @return whether a value exists at the path
     */
    boolean contains(@NotNull String path);

    /**
     * Returns the raw value at the given path.
     *
     * @param path dotted path
     * @return the raw value, or {@code null} if absent
     */
    @Nullable Object get(@NotNull String path);

    /**
     * Returns the string value at the given path.
     *
     * @param path dotted path
     * @return the string value, or {@code null} if absent
     */
    @Nullable String getString(@NotNull String path);

    /**
     * Returns the string value at the given path, falling back to a default.
     *
     * @param path dotted path
     * @param def  default if absent
     * @return the string value, or {@code def}
     */
    @NotNull String getString(@NotNull String path, @NotNull String def);

    /**
     * Returns the int value at the given path.
     *
     * @param path dotted path
     * @return the int value, or {@code 0} if absent or unparseable
     */
    int getInt(@NotNull String path);

    /**
     * Returns the int value at the given path, falling back to a default.
     *
     * @param path dotted path
     * @param def  default if absent
     * @return the int value, or {@code def}
     */
    int getInt(@NotNull String path, int def);

    /**
     * Returns the long value at the given path.
     *
     * @param path dotted path
     * @return the long value, or {@code 0} if absent
     */
    long getLong(@NotNull String path);

    /**
     * Returns the long value at the given path, falling back to a default.
     *
     * @param path dotted path
     * @param def  default if absent
     * @return the long value, or {@code def}
     */
    long getLong(@NotNull String path, long def);

    /**
     * Returns the double value at the given path.
     *
     * @param path dotted path
     * @return the double value, or {@code 0} if absent
     */
    double getDouble(@NotNull String path);

    /**
     * Returns the double value at the given path, falling back to a default.
     *
     * @param path dotted path
     * @param def  default if absent
     * @return the double value, or {@code def}
     */
    double getDouble(@NotNull String path, double def);

    /**
     * Returns the boolean value at the given path. Only the string {@code "true"}
     * (case-insensitive) is treated as {@code true}; YAML 1.1 aliases like
     * {@code "yes"}, {@code "on"}, or {@code "1"} are not.
     *
     * @param path dotted path
     * @return the boolean value, or {@code false} if absent
     */
    boolean getBoolean(@NotNull String path);

    /**
     * Returns the boolean value at the given path, falling back to a default.
     *
     * @param path dotted path
     * @param def  default if absent
     * @return the boolean value, or {@code def}
     */
    boolean getBoolean(@NotNull String path, boolean def);

    /**
     * Returns the string list at the given path.
     *
     * @param path dotted path
     * @return the string list, or an empty list if absent
     */
    @NotNull List<String> getStringList(@NotNull String path);

    /**
     * Returns a section view of the nested map at the given path.
     *
     * @param path dotted path
     * @return a section view of the nested map, or {@code null} if absent or not a map
     */
    @Nullable ConfigSection getSection(@NotNull String path);

    /**
     * Returns the immediate child keys of this section.
     *
     * @return the immediate child keys
     */
    @NotNull Set<String> keys();

    /**
     * Sets a value at a dotted path, creating intermediate sections as needed.
     *
     * @param path  dotted path
     * @param value the value ({@code null} removes the key)
     */
    void set(@NotNull String path, @Nullable Object value);
}
