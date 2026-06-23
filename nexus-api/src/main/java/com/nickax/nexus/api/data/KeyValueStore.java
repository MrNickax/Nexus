package com.nickax.nexus.api.data;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Async key-value operations shared by the cache tier and the backend tier.
 * Keys are already in their string form (see {@link KeyMapper}); values are
 * decoded objects. Every operation runs off the calling thread.
 *
 * @param <V> the value type
 */
public interface KeyValueStore<V> {

    /**
     * Gets the value for a key.
     *
     * @param key the string key
     * @return a future of the value, empty if absent
     */
    @NotNull CompletableFuture<Optional<V>> get(@NotNull String key);

    /**
     * Stores a value under a key, overwriting any existing value.
     *
     * @param key   the string key
     * @param value the value
     * @return a future completing when stored
     */
    @NotNull CompletableFuture<Void> put(@NotNull String key, @NotNull V value);

    /**
     * Removes the value for a key, if present.
     *
     * @param key the string key
     * @return a future completing when removed
     */
    @NotNull CompletableFuture<Void> remove(@NotNull String key);

    /**
     * Whether a value exists for a key.
     *
     * @param key the string key
     * @return a future of whether the key is present
     */
    @NotNull CompletableFuture<Boolean> contains(@NotNull String key);

    /**
     * Reads every entry.
     *
     * @return a future of all entries keyed by their string key
     */
    @NotNull CompletableFuture<Map<String, V>> all();
}
