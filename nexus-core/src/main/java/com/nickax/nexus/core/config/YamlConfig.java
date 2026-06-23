package com.nickax.nexus.core.config;

import com.nickax.nexus.api.config.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * File-backed YAML {@link Config} that preserves comments and key order. On load, keys are
 * ordered to match the bundled default (user values win, user-only keys are kept at the
 * end of their section), missing keys are merged in, and comments are taken from the file
 * (falling back to the default), so a regenerated file keeps its documentation and layout
 * instead of being scrambled by a plain SnakeYAML dump.
 */
public final class YamlConfig extends MapConfigSection implements Config {

    private final Path file;
    private Map<String, String> comments;

    /**
     * Private constructor; use {@link #load(Path, InputStream)} instead.
     *
     * @param file     the config file path
     * @param values   the merged value tree
     * @param comments the merged dotted-key to comment-block map
     */
    private YamlConfig(@NotNull Path file, @NotNull Map<String, Object> values, @NotNull Map<String, String> comments) {
        super(values);
        this.file = file;
        this.comments = comments;
    }

    /**
     * Loads (or creates) a YAML config, merging in any missing keys and comments from the
     * bundled defaults and rewriting the file if its rendered form (order + comments)
     * differs from what is on disk.
     *
     * @param file     the file path
     * @param defaults bundled default YAML stream, or {@code null}
     * @return the loaded config
     */
    public static @NotNull YamlConfig load(@NotNull Path file, @Nullable InputStream defaults) {
        String defaultText = defaults != null ? readStream(defaults) : "";
        Map<String, Object> defaultValues = parseValues(defaultText);
        Map<String, String> defaultComments = YamlComments.parse(lines(defaultText));

        String fileText = readFile(file);

        Map<String, Object> values;
        Map<String, String> comments;
        if (fileText == null) {
            values = defaultValues;
            comments = defaultComments;
        } else {
            values = mergeValues(defaultValues, parseValues(fileText));
            comments = mergeComments(defaultComments, YamlComments.parse(lines(fileText)));
        }

        YamlConfig config = new YamlConfig(file, values, comments);
        String rendered = YamlWriter.write(values, comments);
        if (!rendered.equals(fileText)) {
            config.write(rendered);
        }
        return config;
    }

    /**
     * Writes the current state back to the file, preserving comments and order.
     */
    @Override
    public void save() {
        write(YamlWriter.write(map, comments));
    }

    /**
     * Reloads values and comments from the file, keeping the in-memory values if the file
     * is missing.
     */
    @Override
    public void reload() {
        String text = readFile(file);
        if (text == null) {
            System.getLogger("Nexus").log(System.Logger.Level.WARNING,
                    "Config file missing on reload; keeping in-memory values: " + file);
            return;
        }
        map.clear();
        map.putAll(parseValues(text));
        comments = YamlComments.parse(lines(text));
    }

    /**
     * Merges default values into the existing ones: the result follows the default key
     * order (recursing into nested sections), the existing value wins where present, missing
     * default keys are added, and keys present only in the existing file are appended.
     *
     * @param defaults the default value tree
     * @param existing the existing value tree
     * @return the merged value tree
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> mergeValues(Map<String, Object> defaults, Map<String, Object> existing) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String key = entry.getKey();
            Object defaultValue = entry.getValue();
            Object existingValue = existing.get(key);
            if (defaultValue instanceof Map<?, ?> defaultMap && existingValue instanceof Map<?, ?> existingMap) {
                result.put(key, mergeValues((Map<String, Object>) defaultMap, (Map<String, Object>) existingMap));
            } else if (existing.containsKey(key)) {
                result.put(key, existingValue);
            } else {
                result.put(key, defaultValue);
            }
        }
        for (Map.Entry<String, Object> entry : existing.entrySet()) {
            if (!defaults.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Merges comments: defaults provide the baseline, existing-file comments win where present.
     *
     * @param defaults the default comments
     * @param existing the existing-file comments
     * @return the merged comments
     */
    private static Map<String, String> mergeComments(Map<String, String> defaults, Map<String, String> existing) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>(defaults);
        result.putAll(existing);
        return result;
    }

    /**
     * Parses YAML text into a nested value map (empty for blank input). SnakeYAML preserves
     * the document's key order via {@link LinkedHashMap}.
     *
     * @param text the YAML text
     * @return the parsed nested map
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseValues(String text) {
        if (text == null || text.isBlank()) {
            return new LinkedHashMap<>();
        }
        Object loaded = new Yaml().load(text);
        return loaded instanceof Map<?, ?> map ? (Map<String, Object>) map : new LinkedHashMap<>();
    }

    /**
     * Splits text into lines (handling both {@code \n} and {@code \r\n}).
     *
     * @param text the text, or {@code null}
     * @return the lines
     */
    private static List<String> lines(String text) {
        return text == null ? List.of() : text.lines().toList();
    }

    /**
     * Reads a file to text, or {@code null} if it does not exist.
     *
     * @param file the file
     * @return the file text, or {@code null}
     */
    private static String readFile(Path file) {
        if (!Files.exists(file)) {
            return null;
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read config " + file, e);
        }
    }

    /**
     * Reads a stream fully to text, closing it.
     *
     * @param in the stream
     * @return the text
     */
    private static String readStream(InputStream in) {
        try (in) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read default config", e);
        }
    }

    /**
     * Writes text to the config file, creating parent directories as needed.
     *
     * @param text the text to write
     */
    private void write(String text) {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save config " + file, e);
        }
    }
}
