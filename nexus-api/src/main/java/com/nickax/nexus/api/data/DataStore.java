package com.nickax.nexus.api.data;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Typed, async data access over a cache tier and a durable backend tier. Reads
 * check the cache first and fall back to the backend (populating the cache).
 * Writes update the cache and propagate to the backend per the store's
 * {@link WritePolicy}.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public interface DataStore<K, V> {

    /**
     * Finds a value, checking the cache then the backend.
     *
     * @param key the key
     * @return a future of the value, empty if absent in both tiers
     */
    @NotNull CompletableFuture<Optional<V>> find(@NotNull K key);

    /**
     * Finds a value or creates, stores and returns a new one.
     *
     * @param key     the key
     * @param factory supplies a new value when absent
     * @return a future of the existing or newly created value
     */
    @NotNull CompletableFuture<V> getOrCreate(@NotNull K key, @NotNull Supplier<V> factory);

    /**
     * Stores a value.
     *
     * @param key   the key
     * @param value the value
     * @return a future completing when stored (in the cache, and the backend per policy)
     */
    @NotNull CompletableFuture<Void> save(@NotNull K key, @NotNull V value);

    /**
     * Atomically reads, mutates and stores the value under a per-key lock.
     *
     * @param key     the key
     * @param mutator transforms the current value into the new value
     * @return a future of the new value; completes exceptionally if the key is absent
     */
    @NotNull CompletableFuture<V> update(@NotNull K key, @NotNull UnaryOperator<V> mutator);

    /**
     * Deletes a value from both tiers.
     *
     * @param key the key
     * @return a future completing when deleted
     */
    @NotNull CompletableFuture<Void> delete(@NotNull K key);

    /**
     * Whether a value exists in either tier.
     *
     * @param key the key
     * @return a future of presence
     */
    @NotNull CompletableFuture<Boolean> contains(@NotNull K key);

    /**
     * Loads a value from the backend into the cache.
     *
     * @param key the key
     * @return a future of the loaded value, empty if absent in the backend
     */
    @NotNull CompletableFuture<Optional<V>> load(@NotNull K key);

    /**
     * Flushes all cached values to the backend.
     *
     * @return a future completing when flushed
     */
    @NotNull CompletableFuture<Void> flush();

    /**
     * Reads every value, merging cache and backend (cache wins).
     *
     * @return a future of all values keyed by their typed key
     */
    @NotNull CompletableFuture<Map<K, V>> all();
}
