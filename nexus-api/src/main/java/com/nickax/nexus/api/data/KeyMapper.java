package com.nickax.nexus.api.data;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Bijective mapping between a typed key and its stable string form. The string
 * form is used as the cache key, the backend record id, and the lock key, so it
 * must round-trip exactly.
 *
 * @param <K> the key type
 */
public interface KeyMapper<K> {

    /**
     * Converts a key to its string form.
     *
     * @param key the key
     * @return the string form
     */
    @NotNull String toKey(@NotNull K key);

    /**
     * Parses a key from its string form.
     *
     * @param key the string form
     * @return the typed key
     */
    @NotNull K fromKey(@NotNull String key);

    /**
     * Returns a mapper for {@link UUID} keys. The string form is the standard
     * {@link UUID#toString()} representation.
     *
     * @return a mapper for {@link UUID} keys
     */
    static @NotNull KeyMapper<UUID> uuid() {
        return new KeyMapper<>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public @NotNull String toKey(@NotNull UUID key) {
                return key.toString();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public @NotNull UUID fromKey(@NotNull String key) {
                return UUID.fromString(key);
            }
        };
    }

    /**
     * Returns an identity mapper for {@link String} keys. The string form is used
     * directly as a file name by the file backend, so it must be a valid single path
     * segment (no {@code /}, {@code \}, {@code ..}, or OS-reserved characters).
     *
     * @return an identity mapper for {@link String} keys
     */
    static @NotNull KeyMapper<String> string() {
        return new KeyMapper<>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public @NotNull String toKey(@NotNull String key) {
                return key;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public @NotNull String fromKey(@NotNull String key) {
                return key;
            }
        };
    }

    /**
     * Returns a mapper for {@link Integer} keys. The string form is the decimal
     * representation produced by {@link Integer#toString()}.
     *
     * @return a mapper for {@link Integer} keys
     */
    static @NotNull KeyMapper<Integer> integer() {
        return new KeyMapper<>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public @NotNull String toKey(@NotNull Integer key) {
                return key.toString();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public @NotNull Integer fromKey(@NotNull String key) {
                return Integer.parseInt(key);
            }
        };
    }

    /**
     * Returns a mapper for {@link Long} keys. The string form is the decimal
     * representation produced by {@link Long#toString()}.
     *
     * @return a mapper for {@link Long} keys
     */
    static @NotNull KeyMapper<Long> longKey() {
        return new KeyMapper<>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public @NotNull String toKey(@NotNull Long key) {
                return key.toString();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public @NotNull Long fromKey(@NotNull String key) {
                return Long.parseLong(key);
            }
        };
    }
}
