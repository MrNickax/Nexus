package com.nickax.nexus.api.command;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in {@link ArgumentType}s. Platform-specific types (player, world) are
 * provided by the Bukkit module.
 */
public final class Arguments {

    /**
     * Private constructor — utility class, not instantiable.
     */
    private Arguments() {
    }

    /**
     * @return a single-token string argument
     */
    public static @NotNull ArgumentType<String> word() {
        return input -> input;
    }

    /**
     * @return an argument consuming all remaining tokens, joined by spaces
     */
    public static @NotNull ArgumentType<String> greedyString() {
        return new ArgumentType<>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public @NotNull String parse(@NotNull String input) {
                return input;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean greedy() {
                return true;
            }
        };
    }

    /**
     * @param min minimum (inclusive)
     * @param max maximum (inclusive)
     * @return an integer argument constrained to [min, max]
     */
    public static @NotNull ArgumentType<Integer> integer(int min, int max) {
        return input -> {
            int value;
            try {
                value = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException("'" + input + "' is not a number");
            }
            if (value < min || value > max) {
                throw new ArgumentParseException("must be between " + min + " and " + max);
            }
            return value;
        };
    }

    /**
     * @return a boolean argument suggesting true/false
     */
    public static @NotNull ArgumentType<Boolean> bool() {
        return new ArgumentType<>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public @NotNull Boolean parse(@NotNull String input) throws ArgumentParseException {
                if (input.equalsIgnoreCase("true")) return Boolean.TRUE;
                if (input.equalsIgnoreCase("false")) return Boolean.FALSE;
                throw new ArgumentParseException("'" + input + "' is not true/false");
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public @NotNull List<String> suggest(@NotNull String partial) {
                List<String> all = List.of("true", "false");
                if (partial.isEmpty()) {
                    return all;
                }
                List<String> out = new ArrayList<>();
                for (String value : all) {
                    if (value.startsWith(partial.toLowerCase())) {
                        out.add(value);
                    }
                }
                return out;
            }
        };
    }

    /**
     * @param type the enum class
     * @param <E>  the enum type
     * @return an argument parsing an enum constant case-insensitively, suggesting all constants
     */
    public static <E extends Enum<E>> @NotNull ArgumentType<E> enumValue(@NotNull Class<E> type) {
        return new ArgumentType<>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public @NotNull E parse(@NotNull String input) throws ArgumentParseException {
                for (E constant : type.getEnumConstants()) {
                    if (constant.name().equalsIgnoreCase(input)) {
                        return constant;
                    }
                }
                throw new ArgumentParseException("'" + input + "' is not a valid " + type.getSimpleName());
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public @NotNull List<String> suggest(@NotNull String partial) {
                List<String> out = new ArrayList<>();
                for (E constant : type.getEnumConstants()) {
                    if (constant.name().toLowerCase().startsWith(partial.toLowerCase())) {
                        out.add(constant.name());
                    }
                }
                return out;
            }
        };
    }
}
