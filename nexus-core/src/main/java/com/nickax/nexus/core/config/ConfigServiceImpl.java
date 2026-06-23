package com.nickax.nexus.core.config;

import com.nickax.nexus.api.config.Config;
import com.nickax.nexus.api.config.ConfigService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Default {@link ConfigService}: loads {@link YamlConfig}s.
 */
public final class ConfigServiceImpl implements ConfigService {

    /**
     * Loads a YAML config from the given file, merging any missing keys from the
     * defaults stream and writing them back to disk.
     *
     * @param file     the config file path (created with defaults if absent)
     * @param defaults a stream of default YAML, or {@code null} for no defaults
     * @return the loaded config
     */
    @Override
    public @NotNull Config load(@NotNull Path file, @Nullable InputStream defaults) {
        return YamlConfig.load(file, defaults);
    }
}
