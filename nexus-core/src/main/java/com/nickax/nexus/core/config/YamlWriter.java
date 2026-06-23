package com.nickax.nexus.core.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.Collections;
import java.util.Map;

/**
 * Renders a nested config map to YAML text, re-emitting the comment block stored for each
 * key (which SnakeYAML's value dump would otherwise drop) and writing keys in the map's
 * own order. Scalar and list values are still serialized with SnakeYAML for correctness.
 */
final class YamlWriter {

    private static final Yaml YAML = create();

    private YamlWriter() {
    }

    /**
     * Renders the config tree to YAML text.
     *
     * @param root     the nested value map (insertion order is preserved in the output)
     * @param comments dotted key path to comment block (as produced by {@link YamlComments})
     * @return the YAML document text
     */
    static @NotNull String write(@NotNull Map<String, Object> root, @NotNull Map<String, String> comments) {
        StringBuilder builder = new StringBuilder();
        writeSection(builder, root, comments, "", 0);
        return builder.toString();
    }

    /**
     * Writes a section (one map level) recursively.
     *
     * @param builder   the output buffer
     * @param section   the map level to write
     * @param comments  the comment map
     * @param parentKey the dotted path of the parent, or empty at the root
     * @param depth     the indentation depth
     */
    @SuppressWarnings("unchecked")
    private static void writeSection(StringBuilder builder, Map<String, Object> section,
                                     Map<String, String> comments, String parentKey, int depth) {
        String indent = "  ".repeat(depth);

        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String key = parentKey.isEmpty() ? entry.getKey() : parentKey + "." + entry.getKey();
            writeComment(builder, comments.get(key), indent);

            Object value = entry.getValue();
            if (value instanceof Map<?, ?> child) {
                builder.append(indent).append(entry.getKey()).append(':');
                if (child.isEmpty()) {
                    builder.append(" {}");
                }
                builder.append('\n');
                writeSection(builder, (Map<String, Object>) child, comments, key, depth + 1);
            } else {
                String dumped = YAML.dump(Collections.singletonMap(entry.getKey(), value));
                builder.append(indent)
                        .append(dumped.substring(0, dumped.length() - 1).replace("\n", "\n" + indent))
                        .append('\n');
            }
        }
    }

    /**
     * Writes a comment block, indenting non-blank lines and keeping blank lines (used to
     * preserve the spacing between sections).
     *
     * @param builder the output buffer
     * @param comment the comment block, or {@code null} if the key has no comment
     * @param indent  the indentation for this level
     */
    private static void writeComment(StringBuilder builder, @Nullable String comment, String indent) {
        if (comment == null) {
            return;
        }
        for (String line : comment.split("\n", -1)) {
            if (line.isEmpty()) {
                builder.append('\n');
            } else {
                builder.append(indent).append(line).append('\n');
            }
        }
    }

    /**
     * Builds the block-style, unicode-friendly YAML used to dump individual scalar/list values.
     *
     * @return the configured Yaml instance
     */
    private static Yaml create() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowUnicode(true);
        return new Yaml(options);
    }
}
