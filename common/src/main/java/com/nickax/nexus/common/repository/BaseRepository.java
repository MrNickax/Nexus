package com.nickax.nexus.common.repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Base repository interface for asynchronous key-value storage operations.
 *
 * @param <T> the type of values stored in this repository
 */
public interface BaseRepository<T> {

    /**
     * Checks if the repository contains a value for the specified key.
     *
     * @param key the key to check
     * @return a CompletableFuture that completes with true if the key exists, false otherwise
     */
    default CompletableFuture<Boolean> contains(String key) {
        return get(key).thenApply(Objects::nonNull);
    }

    /**
     * Retrieves the value associated with the specified key or returns a default value if the key is not found.
     *
     * @param key          the key whose associated value is to be returned
     * @param defaultValue the default value to return if the key is not found
     * @return a CompletableFuture that completes with the value associated with the key, or the default value
     */
    default CompletableFuture<T> getOrDefault(String key, T defaultValue) {
        return get(key).thenApply(value -> value != null ? value : defaultValue);
    }

    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key the key whose associated value is to be returned
     * @return a CompletableFuture that completes with the value associated with the key, or null if not found
     */
    CompletableFuture<T> get(String key);

    /**
     * Updates the value associated with the specified key using the provided update function.
     *
     * @param key            the key whose value is to be updated
     * @param updateFunction the function to apply to the current value
     * @return a CompletableFuture that completes with the updated value
     */
    CompletableFuture<T> update(String key, Function<T, T> updateFunction);

    /**
     * Associates the specified value with the specified key in this repository.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return a CompletableFuture that completes with the previous value associated with the key, or null if there was no mapping
     */
    CompletableFuture<T> put(String key, T value);

    /**
     * Associates the specified value with the specified key only if the key is not already associated with a value.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return a CompletableFuture that completes with the current value associated with the key, or null if there was no mapping
     */
    CompletableFuture<T> putIfAbsent(String key, T value);

    /**
     * Removes the mapping for the specified key from this repository.
     *
     * @param key the key whose mapping is to be removed
     * @return a CompletableFuture that completes with the value previously associated with the key, or null if there was no mapping
     */
    CompletableFuture<T> remove(String key);

    /**
     * Checks if the repository is empty.
     *
     * @return a CompletableFuture that completes with true if the repository contains no entries, false otherwise
     */
    default CompletableFuture<Boolean> isEmpty() {
        return size().thenApply(size -> size == 0);
    }

    /**
     * Returns the number of entries in this repository.
     *
     * @return a CompletableFuture that completes with the number of entries
     */
    default CompletableFuture<Integer> size() {
        return getKeys().thenApply(Set::size);
    }

    /**
     * Returns all keys currently stored in this repository.
     *
     * @return a CompletableFuture that completes with a set of all keys
     */
    CompletableFuture<Set<String>> getKeys();

    /**
     * Returns all entries in this repository.
     *
     * @return a CompletableFuture that completes with a map of all key-value pairs
     */
    CompletableFuture<Map<String, T>> getAll();

    /**
     * Stores all entries from the specified map in this repository.
     *
     * @param entries the map containing key-value pairs to be stored
     * @return a CompletableFuture that completes when all entries have been stored
     */
    CompletableFuture<Void> putAll(Map<String, T> entries);

    /**
     * Removes all mappings for the specified keys from this repository.
     *
     * @param keys the list of keys whose mappings are to be removed
     * @return a CompletableFuture that completes when all specified keys have been removed
     */
    CompletableFuture<Void> removeAll(List<String> keys);

    /**
     * Shuts down this repository and releases any resources.
     */
    void shutdown();
}