package com.nickax.nexus.api.config;

/**
 * A file-backed {@link ConfigSection}. Obtain via {@code nexus.configs().load(...)}.
 */
public interface Config extends ConfigSection {

    /**
     * Writes the current state back to the backing file.
     */
    void save();

    /**
     * Reloads the state from the backing file, discarding unsaved changes.
     */
    void reload();
}
