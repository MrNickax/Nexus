package com.nickax.nexus.core.config;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Extracts the comment block that precedes each key in a YAML document and maps it to the
 * key's dotted path. A "comment block" is the run of {@code #} lines and blank lines
 * immediately above a key, so the blank-line spacing between sections is preserved too.
 * Used so the config writer can re-emit comments that SnakeYAML's value dump drops.
 */
final class YamlComments {

    private YamlComments() {
    }

    /**
     * Parses the comment blocks from YAML lines.
     *
     * @param lines the document lines
     * @return a map of dotted key path to the raw comment block (lines joined with {@code \n},
     *         no trailing newline); keys with no preceding comment are absent
     */
    static @NotNull LinkedHashMap<String, String> parse(@NotNull List<String> lines) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        Deque<PathPart> path = new ArrayDeque<>();
        List<String> pending = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                pending.add(trimmed);
                continue;
            }

            if (trimmed.startsWith("-") || !trimmed.contains(":")) {
                continue;
            }

            int indentation = countIndentation(line);
            String keyPart = trimmed.substring(0, trimmed.indexOf(':')).trim();

            while (!path.isEmpty() && path.peekLast().indentation() >= indentation) {
                path.removeLast();
            }
            path.addLast(new PathPart(indentation, keyPart));

            if (!pending.isEmpty()) {
                result.put(buildKey(path), String.join("\n", pending));
                pending.clear();
            }
        }

        return result;
    }

    /**
     * Counts the leading spaces of a line.
     *
     * @param line the line
     * @return the number of leading spaces
     */
    private static int countIndentation(String line) {
        int indentation = 0;
        while (indentation < line.length() && line.charAt(indentation) == ' ') {
            indentation++;
        }
        return indentation;
    }

    /**
     * Joins the current path stack into a dotted key.
     *
     * @param path the path stack
     * @return the dotted key
     */
    private static String buildKey(Deque<PathPart> path) {
        StringBuilder builder = new StringBuilder();
        for (PathPart part : path) {
            if (!builder.isEmpty()) {
                builder.append('.');
            }
            builder.append(part.key());
        }
        return builder.toString();
    }

    /**
     * One level of the current key path.
     *
     * @param indentation the indentation of the key
     * @param key         the key name
     */
    private record PathPart(int indentation, String key) {
    }
}
