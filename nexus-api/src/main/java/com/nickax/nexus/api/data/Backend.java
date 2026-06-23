package com.nickax.nexus.api.data;

/**
 * The durable tier of a {@link DataStore}: where values survive a restart.
 *
 * @param <V> the value type
 */
public interface Backend<V> extends KeyValueStore<V> {
}
