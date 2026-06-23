package com.nickax.nexus.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlConfigTest {

    private static ByteArrayInputStream yaml(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void load_createsFileFromDefaults_whenMissing(@TempDir Path dir) {
        Path file = dir.resolve("config.yml");
        YamlConfig config = YamlConfig.load(file, yaml("name: nexus\nlimits:\n  max: 10\n"));
        assertEquals("nexus", config.getString("name"));
        assertEquals(10, config.getInt("limits.max"));
        assertTrue(Files.exists(file));
    }

    @Test
    void load_mergesMissingDefaultKeys_andWritesBack(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.yml");
        Files.writeString(file, "name: custom\n");
        YamlConfig config = YamlConfig.load(file, yaml("name: nexus\nlimits:\n  max: 10\n"));
        // existing value kept, missing default added
        assertEquals("custom", config.getString("name"));
        assertEquals(10, config.getInt("limits.max"));
        // written back to disk
        String onDisk = Files.readString(file);
        assertTrue(onDisk.contains("max"));
    }

    @Test
    void save_thenReload_persistsChanges(@TempDir Path dir) {
        Path file = dir.resolve("config.yml");
        YamlConfig config = YamlConfig.load(file, yaml("count: 1\n"));
        config.set("count", 99);
        config.save();
        config.reload();
        assertEquals(99, config.getInt("count"));
    }

    @Test
    void load_withoutDefaults_emptyWhenMissing(@TempDir Path dir) {
        YamlConfig config = YamlConfig.load(dir.resolve("none.yml"), null);
        assertFalse(config.contains("anything"));
    }

    @Test
    void load_preservesCommentsFromDefaults(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.yml");
        String defaults = "# top comment\nname: nexus\n# nested section\nlimits:\n  # the max\n  max: 10\n";
        YamlConfig.load(file, yaml(defaults));

        String onDisk = Files.readString(file);
        assertTrue(onDisk.contains("# top comment"), "header comment preserved");
        assertTrue(onDisk.contains("# nested section"), "section comment preserved");
        assertTrue(onDisk.contains("# the max"), "nested comment preserved");
    }

    @Test
    void load_reordersKeysToMatchDefault(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.yml");
        Files.writeString(file, "b: 2\na: 1\n"); // out of order, missing c
        YamlConfig.load(file, yaml("a: 0\nb: 0\nc: 3\n"));

        String onDisk = Files.readString(file);
        assertTrue(onDisk.indexOf("a:") < onDisk.indexOf("b:"), "a before b (default order)");
        assertTrue(onDisk.indexOf("b:") < onDisk.indexOf("c:"), "b before c (default order)");
        // user values kept, missing default added
        assertEquals("1", onDisk.lines().filter(l -> l.startsWith("a:")).findFirst().orElse("").replace("a:", "").trim());
    }
}
