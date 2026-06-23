package com.nickax.nexus.core.data;

import com.google.gson.Gson;
import com.nickax.nexus.api.data.Codec;
import org.jetbrains.annotations.NotNull;

/**
 * {@link Codec} backed by Gson. Values are plain POJOs; fields may use Gson's
 * {@code @SerializedName} for stable names.
 *
 * @param <V> the value type
 */
public final class GsonCodec<V> implements Codec<V> {

    private static final Gson GSON = new Gson();

    private final Class<V> type;

    /**
     * Creates a codec for the given type.
     *
     * @param type the value class
     */
    public GsonCodec(@NotNull Class<V> type) {
        this.type = type;
    }

    /**
     * Serialises the value to a JSON string.
     *
     * @param value the value to encode
     * @return the Gson JSON representation
     */
    @Override
    public @NotNull String encode(@NotNull V value) {
        return GSON.toJson(value);
    }

    /**
     * Deserialises a JSON string back to a value of type {@code V}.
     *
     * @param data the JSON string to decode
     * @return the decoded value
     */
    @Override
    public @NotNull V decode(@NotNull String data) {
        return GSON.fromJson(data, type);
    }
}
