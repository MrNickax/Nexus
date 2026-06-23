package com.nickax.nexus.core.data;

import com.nickax.nexus.api.data.Backend;
import com.nickax.nexus.api.data.Codec;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

/**
 * File-backed {@link Backend}. Each entry is a {@code <key>.json} file under the
 * backing directory, with the value serialised by a {@link Codec}. All IO runs on
 * the supplied executor. This is the zero-configuration durable backend for
 * single-server use. Directory creation runs synchronously in the constructor on
 * the calling thread; all other IO runs on the supplied executor.
 *
 * @param <V> the value type
 */
public final class FileBackend<V> implements Backend<V> {

    private static final String EXTENSION = ".json";

    private final Path directory;
    private final Codec<V> codec;
    private final Executor executor;

    /**
     * Creates a file backend.
     *
     * @param directory the directory to store entry files in (created if absent)
     * @param codec     the value codec
     * @param executor  the executor IO runs on
     */
    public FileBackend(@NotNull Path directory, @NotNull Codec<V> codec, @NotNull Executor executor) {
        this.directory = directory;
        this.codec = codec;
        this.executor = executor;
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create data directory " + directory, e);
        }
    }

    /**
     * Resolves the file path for a key, rejecting keys that could escape the
     * backing directory via path traversal.
     *
     * @param key the entry key
     * @return the {@code .json} file path for this key
     * @throws IllegalArgumentException if the key contains {@code /}, {@code \}, or {@code ..}
     */
    private Path fileOf(String key) {
        if (key.contains("/") || key.contains("\\") || key.contains("..")) {
            throw new IllegalArgumentException("Key is not a safe filename: " + key);
        }
        return directory.resolve(key + EXTENSION);
    }

    /**
     * Reads and decodes the file for {@code key}, returning empty if the file does
     * not exist.
     *
     * @param key the entry key
     * @return a future containing the optional decoded value
     */
    @Override
    public @NotNull CompletableFuture<Optional<V>> get(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> {
            Path file = fileOf(key);
            if (!Files.exists(file)) {
                return Optional.empty();
            }
            try {
                return Optional.of(codec.decode(Files.readString(file, StandardCharsets.UTF_8)));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read " + file, e);
            }
        }, executor);
    }

    /**
     * Encodes and writes the value to a {@code .tmp} file and then moves it
     * atomically to the target path, preventing partial writes from corrupting
     * existing data on crash.
     *
     * @param key   the entry key
     * @param value the value to persist
     * @return a future that completes when the file is written
     */
    @Override
    public @NotNull CompletableFuture<Void> put(@NotNull String key, @NotNull V value) {
        return CompletableFuture.runAsync(() -> {
            Path file = fileOf(key);
            Path tmp = directory.resolve(key + EXTENSION + ".tmp");
            try {
                Files.writeString(tmp, codec.encode(value), StandardCharsets.UTF_8);
                try {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to write " + file, e);
            }
        }, executor);
    }

    /**
     * Deletes the file for {@code key}, if it exists. No-ops silently when absent.
     *
     * @param key the entry key to remove
     * @return a future that completes when the file is gone (or was never present)
     */
    @Override
    public @NotNull CompletableFuture<Void> remove(@NotNull String key) {
        return CompletableFuture.runAsync(() -> {
            Path file = fileOf(key);
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to delete " + file, e);
            }
        }, executor);
    }

    /**
     * Returns whether the file for {@code key} exists on disk.
     *
     * @param key the entry key to test
     * @return a future resolving to {@code true} if the file is present
     */
    @Override
    public @NotNull CompletableFuture<Boolean> contains(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> Files.exists(fileOf(key)), executor);
    }

    /**
     * Lists all {@code .json} files in the directory, decodes each one, and returns
     * the full set of entries keyed by filename (without extension).
     *
     * @return a future containing all entries in the directory
     */
    @Override
    public @NotNull CompletableFuture<Map<String, V>> all() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, V> result = new HashMap<>();
            try (Stream<Path> files = Files.list(directory)) {
                files.filter(p -> p.getFileName().toString().endsWith(EXTENSION)).forEach(p -> {
                    String name = p.getFileName().toString();
                    String key = name.substring(0, name.length() - EXTENSION.length());
                    try {
                        result.put(key, codec.decode(Files.readString(p, StandardCharsets.UTF_8)));
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to read " + p, e);
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to list " + directory, e);
            }
            return result;
        }, executor);
    }
}
