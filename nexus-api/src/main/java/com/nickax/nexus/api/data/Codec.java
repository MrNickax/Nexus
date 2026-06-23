package com.nickax.nexus.api.data;

import org.jetbrains.annotations.NotNull;

/**
 * Serialises a value to a string and back. Backends and caches store the encoded
 * string form; the store hands typed values to callers.
 *
 * @param <V> the value type
 */
public interface Codec<V> {

    /**
     * Encodes a value to its string form.
     *
     * @param value the value to encode
     * @return the encoded string
     */
    @NotNull String encode(@NotNull V value);

    /**
     * Decodes a value from its string form.
     *
     * @param data the encoded string
     * @return the decoded value
     */
    @NotNull V decode(@NotNull String data);
}
