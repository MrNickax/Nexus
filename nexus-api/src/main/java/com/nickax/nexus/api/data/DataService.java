package com.nickax.nexus.api.data;

import org.jetbrains.annotations.NotNull;

/**
 * Factory for {@link DataStore}s. Obtain via {@code nexus.data()}.
 */
public interface DataService {

    /**
     * Begins building a data store with the given logical name and value type.
     * The name scopes the file backend's directory and is used in logs.
     *
     * <p><b>The {@code name} is a GLOBAL namespace shared by every plugin using
     * Nexus</b> — it scopes the file directory, the Mongo collection, the SQL table
     * and the Redis map. Two plugins that pick the same name will share (and
     * corrupt) each other's data. It must match {@code [A-Za-z0-9_]+}; prefix it
     * with your plugin id, e.g. {@code "myplugin_players"}.
     *
     * @param name the logical store name (also the file backend sub-directory)
     * @param type the value class (used for the default Gson codec)
     * @param <K>  the key type
     * @param <V>  the value type
     * @return a fresh builder
     */
    <K, V> @NotNull DataStoreBuilder<K, V> store(@NotNull String name, @NotNull Class<V> type);
}
