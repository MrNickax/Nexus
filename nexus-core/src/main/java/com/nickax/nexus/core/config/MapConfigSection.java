package com.nickax.nexus.core.config;

import com.nickax.nexus.api.config.ConfigSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A {@link ConfigSection} backed by a nested {@code Map<String,Object>}. Dotted
 * paths navigate nested maps; intermediate maps are created on write.
 */
public class MapConfigSection implements ConfigSection {

    /** The backing map (shared with parent sections so writes propagate). */
    protected final Map<String, Object> map;

    /**
     * Wraps a map.
     *
     * @param map the backing map
     */
    public MapConfigSection(@NotNull Map<String, Object> map) {
        this.map = map;
    }

    /**
     * Resolves a dotted path to the raw value it points to, or {@code null} if any
     * segment is absent or the path traverses a non-map node.
     *
     * @param path the dotted key path (e.g. {@code "database.host"})
     * @return the raw value, or {@code null}
     */
    @SuppressWarnings("unchecked")
    private Object resolve(String path) {
        String[] parts = path.split("\\.");
        Object current = map;
        for (String part : parts) {
            if (!(current instanceof Map<?, ?> m)) {
                return null;
            }
            current = ((Map<String, Object>) m).get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /**
     * Returns whether a value exists at the given dotted path, without walking
     * past non-map nodes.
     *
     * @param path the dotted key path
     * @return {@code true} if the key is present
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(@NotNull String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = map;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map<?, ?>)) {
                return false;
            }
            current = (Map<String, Object>) next;
        }
        return current.containsKey(parts[parts.length - 1]);
    }

    /**
     * Returns the raw value at the given path, or {@code null} if absent.
     *
     * @param path the dotted key path
     * @return the raw value, or {@code null}
     */
    @Override
    public @Nullable Object get(@NotNull String path) {
        return resolve(path);
    }

    /**
     * Returns the string value at the given path via {@link String#valueOf}, or
     * {@code null} if the path is absent.
     *
     * @param path the dotted key path
     * @return the string value, or {@code null}
     */
    @Override
    public @Nullable String getString(@NotNull String path) {
        Object value = resolve(path);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Returns the string value at the given path, or {@code def} if absent.
     *
     * @param path the dotted key path
     * @param def  the default value
     * @return the string value, or {@code def}
     */
    @Override
    public @NotNull String getString(@NotNull String path, @NotNull String def) {
        String value = getString(path);
        return value != null ? value : def;
    }

    /**
     * Returns the integer value at the given path, or {@code 0} if absent or unparseable.
     *
     * @param path the dotted key path
     * @return the integer value, or {@code 0}
     */
    @Override
    public int getInt(@NotNull String path) {
        return getInt(path, 0);
    }

    /**
     * Returns the integer value at the given path, or {@code def} if absent or unparseable.
     *
     * @param path the dotted key path
     * @param def  the default value
     * @return the integer value, or {@code def}
     */
    @Override
    public int getInt(@NotNull String path, int def) {
        Object value = resolve(path);
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    /**
     * Returns the long value at the given path, or {@code 0} if absent or unparseable.
     *
     * @param path the dotted key path
     * @return the long value, or {@code 0}
     */
    @Override
    public long getLong(@NotNull String path) {
        return getLong(path, 0L);
    }

    /**
     * Returns the long value at the given path, or {@code def} if absent or unparseable.
     *
     * @param path the dotted key path
     * @param def  the default value
     * @return the long value, or {@code def}
     */
    @Override
    public long getLong(@NotNull String path, long def) {
        Object value = resolve(path);
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    /**
     * Returns the double value at the given path, or {@code 0.0} if absent or unparseable.
     *
     * @param path the dotted key path
     * @return the double value, or {@code 0.0}
     */
    @Override
    public double getDouble(@NotNull String path) {
        return getDouble(path, 0D);
    }

    /**
     * Returns the double value at the given path, or {@code def} if absent or unparseable.
     *
     * @param path the dotted key path
     * @param def  the default value
     * @return the double value, or {@code def}
     */
    @Override
    public double getDouble(@NotNull String path, double def) {
        Object value = resolve(path);
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    /**
     * Returns the boolean value at the given path, or {@code false} if absent.
     *
     * @param path the dotted key path
     * @return the boolean value, or {@code false}
     */
    @Override
    public boolean getBoolean(@NotNull String path) {
        return getBoolean(path, false);
    }

    /**
     * Returns the boolean value at the given path, or {@code def} if absent.
     * String values are parsed with {@link Boolean#parseBoolean}.
     *
     * @param path the dotted key path
     * @param def  the default value
     * @return the boolean value, or {@code def}
     */
    @Override
    public boolean getBoolean(@NotNull String path, boolean def) {
        Object value = resolve(path);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return Boolean.parseBoolean(s.trim());
        }
        return def;
    }

    /**
     * Returns the list at the given path as strings via {@link String#valueOf}, or
     * an empty list if the path is absent or not a list.
     *
     * @param path the dotted key path
     * @return the list of strings, never {@code null}
     */
    @Override
    public @NotNull List<String> getStringList(@NotNull String path) {
        Object value = resolve(path);
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>(list.size());
            for (Object element : list) {
                result.add(String.valueOf(element));
            }
            return result;
        }
        return new ArrayList<>();
    }

    /**
     * Returns a view of the nested map at the given path as a {@link ConfigSection},
     * or {@code null} if the path is absent or not a map.
     *
     * @param path the dotted key path
     * @return the section, or {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public @Nullable ConfigSection getSection(@NotNull String path) {
        Object value = resolve(path);
        return value instanceof Map<?, ?> m ? new MapConfigSection((Map<String, Object>) m) : null;
    }

    /**
     * Returns the top-level keys of this section in insertion order.
     *
     * @return an ordered snapshot of the top-level keys
     */
    @Override
    public @NotNull Set<String> keys() {
        return new LinkedHashSet<>(map.keySet());
    }

    /**
     * Sets the value at the given dotted path, creating intermediate maps as needed.
     * Passing {@code null} removes the key. Throws {@link IllegalArgumentException}
     * if an intermediate path segment exists but is not a map.
     *
     * @param path  the dotted key path
     * @param value the value to set, or {@code null} to remove
     */
    @Override
    @SuppressWarnings("unchecked")
    public void set(@NotNull String path, @Nullable Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = map;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (next == null) {
                Map<String, Object> created = new LinkedHashMap<>();
                current.put(parts[i], created);
                current = created;
            } else if (next instanceof Map<?, ?>) {
                current = (Map<String, Object>) next;
            } else {
                throw new IllegalArgumentException("Cannot set '" + path + "': path segment '"
                        + parts[i] + "' is not a section");
            }
        }
        String leaf = parts[parts.length - 1];
        if (value == null) {
            current.remove(leaf);
        } else {
            current.put(leaf, value);
        }
    }
}
