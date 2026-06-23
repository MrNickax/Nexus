package com.nickax.nexus.api.data;

/**
 * The hot tier of a {@link DataStore}: a fast key-value store sitting in front of
 * a {@link Backend}.
 *
 * @param <V> the value type
 */
public interface Cache<V> extends KeyValueStore<V> {
}
