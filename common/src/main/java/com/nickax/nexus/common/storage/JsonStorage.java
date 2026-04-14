package com.nickax.nexus.common.storage;

import com.nickax.nexus.common.util.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A JSON-based storage implementation that persists data to the file system.
 * <p>
 * This class provides thread-safe, asynchronous storage operations by storing each key-value pair
 * as a separate JSON file. It uses per-key locking to ensure consistency and prevent race conditions
 * when multiple operations are performed on the same key concurrently.
 *
 * @param <T> the type of values stored in this storage
 */
public class JsonStorage<T> extends Storage<T> {

    private final File dataFolder;
    private final Class<T> type;
    private final Logger logger;
    private final Executor executor;

    private final Map<String, CompletableFuture<Void>> keyLocks = new ConcurrentHashMap<>();
    private volatile boolean shuttingDown = false;

    /**
     * Constructs a new JsonStorage instance.
     *
     * @param dataFolder the directory where JSON files will be stored
     * @param type       the class type of the values to be stored
     * @param logger     the logger for error, warning, and info messages
     */
    public JsonStorage(File dataFolder, Class<T> type, Logger logger) {
        this.dataFolder = dataFolder;
        this.type = type;
        this.logger = logger;
        this.executor = JsonStorageExecutor.getJsonExecutor();
        logger.info("JsonStorage initialized for folder: " + dataFolder.getAbsolutePath());
    }

    /**
     * Retrieves the value associated with the specified key asynchronously.
     *
     * @param key the key whose associated value is to be returned
     * @return a CompletableFuture that will complete with the value, or null if no value exists
     */
    @Override
    public CompletableFuture<T> get(String key) {
        return withLock("get", key, () -> {
            File file = getFile(key);
            return file.exists()
                    ? CompletableFuture.completedFuture(getValue(file))
                    : CompletableFuture.completedFuture(null);
        });
    }

    /**
     * Updates the value associated with the specified key using the provided update function.
     * <p>
     * If the update function returns null, the entry will be removed from storage.
     *
     * @param key            the key whose associated value is to be updated
     * @param updateFunction a function that takes the current value (or null) and returns the updated value
     * @return a CompletableFuture that will complete with the updated value, or null if the entry was removed
     */
    @Override
    public CompletableFuture<T> update(String key, Function<T, T> updateFunction) {
        return withLock("update", key, () -> {
            File file = getFile(key);

            T currentValue = file.exists() ? getValue(file) : null;
            T updatedValue = updateFunction.apply(currentValue);

            if (updatedValue == null) {
                if (!file.delete()) {
                    logger.warning("JsonStorage update failed to delete file for key: " + key);
                }
                return CompletableFuture.completedFuture(null);
            }

            saveValue(file, updatedValue);
            return CompletableFuture.completedFuture(updatedValue);
        });
    }

    /**
     * Stores the specified value with the specified key.
     * <p>
     * If a value already exists for the key, it will be overwritten.
     *
     * @param key   the key with which the value is to be associated
     * @param value the value to be stored
     * @return a CompletableFuture that will complete with the stored value
     */
    @Override
    public CompletableFuture<T> put(String key, T value) {
        return withLock("put", key, () -> {
            File file = getFile(key);
            saveValue(file, value);
            return CompletableFuture.completedFuture(value);
        });
    }

    /**
     * Stores the specified value with the specified key only if no value currently exists for that key.
     *
     * @param key   the key with which the value is to be associated
     * @param value the value to be stored
     * @return a CompletableFuture that will complete with the stored value if successful, or null if a value already exists
     */
    @Override
    public CompletableFuture<T> putIfAbsent(String key, T value) {
        return withLock("putIfAbsent", key, () -> {
            File file = getFile(key);

            if (file.exists()) {
                return CompletableFuture.completedFuture(null);
            }

            saveValue(file, value);
            return CompletableFuture.completedFuture(value);
        });
    }

    /**
     * Removes the value associated with the specified key.
     *
     * @param key the key whose associated value is to be removed
     * @return a CompletableFuture that will complete with the removed value, or null if no value existed
     */
    @Override
    public CompletableFuture<T> remove(String key) {
        return withLock("remove", key, () -> {
            File file = getFile(key);

            if (!file.exists()) {
                return CompletableFuture.completedFuture(null);
            }

            T value = getValue(file);

            if (!file.delete()) {
                logger.warning("JsonStorage remove failed to delete file for key: " + key);
            }

            return CompletableFuture.completedFuture(value);
        });
    }

    /**
     * Retrieves all keys currently stored in this storage.
     *
     * @return a CompletableFuture that will complete with a set of all keys
     */
    @Override
    public CompletableFuture<Set<String>> getKeys() {
        return supplyAsync("getKeys", () -> {
            Set<String> keys = new HashSet<>();

            File[] files = dataFolder.listFiles((file, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    keys.add(file.getName().replace(".json", ""));
                }
            }

            return keys;
        });
    }

    /**
     * Retrieves all key-value pairs currently stored in this storage.
     *
     * @return a CompletableFuture that will complete with a map containing all key-value pairs
     */
    @Override
    public CompletableFuture<Map<String, T>> getAll() {
        return supplyAsync("getAll", () -> {
            Map<String, T> result = new HashMap<>();

            File[] files = dataFolder.listFiles((file, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    String key = file.getName().replace(".json", "");
                    result.put(key, getValue(file));
                }
            }

            return result;
        });
    }

    /**
     * Stores all key-value pairs from the specified map.
     *
     * @param entries a map containing the key-value pairs to be stored
     * @return a CompletableFuture that will complete when all entries have been stored
     */
    @Override
    public CompletableFuture<Void> putAll(Map<String, T> entries) {
        if (entries.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<T>> futures = new ArrayList<>(entries.size());
        for (Map.Entry<String, T> entry : entries.entrySet()) {
            futures.add(withLock("putAll", entry.getKey(), () -> {
                File file = getFile(entry.getKey());
                saveValue(file, entry.getValue());
                return CompletableFuture.completedFuture(entry.getValue());
            }));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /**
     * Removes all mappings for the specified keys from this repository.
     *
     * @param keys the list of keys whose mappings are to be removed
     * @return a CompletableFuture that completes when all specified keys have been removed
     */
    @Override
    public CompletableFuture<Void> removeAll(List<String> keys) {
        if (keys.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<T>> futures = new ArrayList<>(keys.size());
        for (String key : keys) {
            futures.add(withLock("removeAll", key, () -> {
                File file = getFile(key);

                if (!file.exists()) {
                    return CompletableFuture.completedFuture(null);
                }

                T value = getValue(file);

                if (!file.delete()) {
                    logger.warning("JsonStorage removeAll failed to delete file for key: " + key);
                }

                return CompletableFuture.completedFuture(value);
            }));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /**
     * Shuts down this storage instance gracefully.
     * <p>
     * This method waits for all pending operations to complete before shutting down.
     * It sets a shutdown flag to prevent new operations from being accepted,
     * waits for all active key locks to be released, and clears the lock map.
     * </p>
     */
    @Override
    public void shutdown() {
        try {
            shuttingDown = true;

            CompletableFuture<?>[] activeLocks = keyLocks.values().toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(activeLocks).join();

            keyLocks.clear();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "JsonStorage shutdown failed", e);
        }
    }
    /**
     * Executes a task asynchronously that returns a result.
     *
     * @param <R>       the type of the result
     * @param operation the name of the operation being performed
     * @param task      the task to execute
     * @return a CompletableFuture containing the result of the task
     */
    private <R> CompletableFuture<R> supplyAsync(String operation, Supplier<R> task) {
        return CompletableFuture.supplyAsync(task, executor)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.log(Level.SEVERE, formatMessage(operation), unwrap(throwable));
                    }
                });
    }

    /**
     * Executes a function with a per-key lock to ensure thread-safe operations.
     *
     * @param operation the name of the operation being performed
     * @param key       the key to lock
     * @param function  the function to execute while holding the lock
     * @param <R>       the type of the result
     * @return a CompletableFuture that will complete with the result of the function
     */
    private <R> CompletableFuture<R> withLock(String operation, String key, Supplier<CompletableFuture<R>> function) {
        if (shuttingDown) {
            logger.warning("JsonStorage " + operation + " rejected because shutdown is in progress for key: " + key);
            return CompletableFuture.failedFuture(new IllegalStateException("JsonStorage is shutting down"));
        }

        return CompletableFuture.supplyAsync(() -> {
            if (shuttingDown) {
                throw new IllegalStateException("JsonStorage is shutting down");
            }

            CompletableFuture<Void> previous = keyLocks.get(key);

            CompletableFuture<R> resultFuture = (previous == null ? CompletableFuture.completedFuture(null) : previous)
                    .handle((ignored, throwable) -> null)
                    .thenCompose(ignored -> {
                        try {
                            return function.get();
                        } catch (Exception e) {
                            CompletableFuture<R> failed = new CompletableFuture<>();
                            failed.completeExceptionally(e);
                            return failed;
                        }
                    });

            keyLocks.put(key, resultFuture.thenApply(ignored -> null));

            return resultFuture.whenComplete((result, throwable) -> {
                keyLocks.remove(key);

                if (throwable != null) {
                    logger.log(Level.SEVERE, "JsonStorage " + operation + " failed for key: " + key, unwrap(throwable));
                }
            });
        }, executor).thenCompose(Function.identity());
    }

    /**
     * Formats a log message for the given operation and key.
     *
     * @param operation the operation name
     * @return a formatted log message
     */
    private String formatMessage(String operation) {
        return "JsonStorage " + operation;
    }

    /**
     * Unwraps a CompletionException to retrieve the underlying cause.
     *
     * @param throwable the throwable to unwrap
     * @return the underlying cause if the throwable is a CompletionException, otherwise the original throwable
     */
    private Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException completionException && completionException.getCause() != null) {
            return completionException.getCause();
        }
        return throwable;
    }

    /**
     * Returns the File object for the specified key.
     *
     * @param key the key
     * @return the File object representing the JSON file for this key
     */
    private File getFile(String key) {
        return new File(dataFolder, key + ".json");
    }

    /**
     * Saves the specified value to the specified file as JSON.
     *
     * @param file  the file to write to
     * @param value the value to save
     * @throws RuntimeException if an I/O error occurs during saving
     */
    private void saveValue(File file, T value) {
        try {
            File parent = file.getParentFile();

            if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
                throw new IOException("Failed to create directory: " + parent.getAbsolutePath());
            }

            String json = JsonUtil.toJson(value);
            Files.writeString(file.toPath(), json);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save JSON file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Reads and deserializes a value from the specified JSON file.
     *
     * @param file the file to read from
     * @return the deserialized value
     * @throws RuntimeException if an I/O error occurs during reading
     */
    public T getValue(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return JsonUtil.fromJson(new String(bytes, StandardCharsets.UTF_8), type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file: " + file.getAbsolutePath(), e);
        }
    }
}