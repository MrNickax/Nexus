package com.nickax.nexus.core.data;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.nickax.nexus.api.data.Backend;
import com.nickax.nexus.api.data.Codec;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * MongoDB {@link Backend}. Each entry is a document {@code {_id: key, value: <encoded>}}
 * in the store's collection. The blocking sync-driver calls run on the supplied executor.
 *
 * @param <V> the value type
 */
public final class MongoBackend<V> implements Backend<V> {

    private static final String VALUE_FIELD = "value";

    private final MongoCollection<Document> collection;
    private final Codec<V> codec;
    private final Executor executor;

    /**
     * Creates a Mongo backend over a collection.
     *
     * @param collection the store's collection
     * @param codec      the value codec
     * @param executor   the executor blocking calls run on
     */
    public MongoBackend(@NotNull MongoCollection<Document> collection, @NotNull Codec<V> codec,
                        @NotNull Executor executor) {
        this.collection = collection;
        this.codec = codec;
        this.executor = executor;
    }

    /**
     * Fetches the document with {@code _id == key} and decodes its {@code value} field.
     *
     * @param key the entry key
     * @return a future containing the optional decoded value
     */
    @Override
    public @NotNull CompletableFuture<Optional<V>> get(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(Filters.eq("_id", key)).first();
            return doc == null ? Optional.empty() : Optional.of(codec.decode(doc.getString(VALUE_FIELD)));
        }, executor);
    }

    /**
     * Upserts the value as a document {@code {_id: key, value: <encoded>}}.
     *
     * @param key   the entry key
     * @param value the value to persist
     * @return a future that completes when the upsert is done
     */
    @Override
    public @NotNull CompletableFuture<Void> put(@NotNull String key, @NotNull V value) {
        return CompletableFuture.runAsync(() -> {
            Document doc = new Document("_id", key).append(VALUE_FIELD, codec.encode(value));
            collection.replaceOne(Filters.eq("_id", key), doc, new ReplaceOptions().upsert(true));
        }, executor);
    }

    /**
     * Deletes the document for {@code key}, if present.
     *
     * @param key the entry key to remove
     * @return a future that completes when the delete is done
     */
    @Override
    public @NotNull CompletableFuture<Void> remove(@NotNull String key) {
        return CompletableFuture.runAsync(() -> collection.deleteOne(Filters.eq("_id", key)), executor);
    }

    /**
     * Returns whether a document with {@code _id == key} exists in the collection.
     *
     * @param key the entry key to test
     * @return a future resolving to {@code true} if the document is present
     */
    @Override
    public @NotNull CompletableFuture<Boolean> contains(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> collection.countDocuments(Filters.eq("_id", key)) > 0, executor);
    }

    /**
     * Reads and decodes every document in the collection.
     *
     * @return a future containing all entries keyed by their {@code _id}
     */
    @Override
    public @NotNull CompletableFuture<Map<String, V>> all() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, V> result = new HashMap<>();
            try (MongoCursor<Document> cursor = collection.find().iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    result.put(doc.getString("_id"), codec.decode(doc.getString(VALUE_FIELD)));
                }
            }
            return result;
        }, executor);
    }
}
