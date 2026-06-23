package com.nickax.nexus.api.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loads {@link Config}s. Obtain via {@code nexus.configs()}.
 */
public interface ConfigService {

    /**
     * Loads (or creates) a config file. If {@code defaults} is non-null, the file is
     * created from it when missing, and any keys present in the defaults but absent in
     * the file are merged in and written back.
     *
     * @param file     the config file path (e.g. {@code dataFolder/config.yml})
     * @param defaults a stream of the bundled default YAML, or {@code null}
     * @return the loaded config
     */
    @NotNull Config load(@NotNull Path file, @Nullable InputStream defaults);
}
