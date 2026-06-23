package com.nickax.nexus.core.config;

import com.nickax.nexus.api.config.Config;
import com.nickax.nexus.core.NexusImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigServiceImplTest {

    @Test
    void nexusConfigs_loadsAConfig(@TempDir Path dir) {
        NexusImpl nexus = new NexusImpl(dir);
        try {
            Config config = nexus.configs().load(dir.resolve("c.yml"),
                    new ByteArrayInputStream("greeting: hi\n".getBytes(StandardCharsets.UTF_8)));
            assertEquals("hi", config.getString("greeting"));
        } finally {
            nexus.shutdown();
        }
    }
}
