package com.nickax.nexus.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Utility class for JSON serialization and deserialization using Gson.
 * Provides methods to convert objects to JSON strings and parse JSON strings into objects.
 * Supports configurable Gson behavior through configurators.
 */
public class JsonUtil {

    private static volatile Gson gson = new Gson();
    private static final Map<String, Consumer<GsonBuilder>> configurators = new ConcurrentHashMap<>();

    private static final Object lock = new Object();

    private JsonUtil() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    /**
     * Converts an object to its JSON string representation.
     *
     * @param object The object to convert to JSON
     * @return JSON string representation of the object
     */
    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    /**
     * Parses a JSON string into an object of the specified type.
     *
     * @param json The JSON string to parse
     * @param type The class of the target object
     * @param <T>  The type of the target object
     * @return An instance of type T parsed from the JSON string
     */
    public static <T> T fromJson(String json, Class<T> type) {
        return gson.fromJson(json, type);
    }

    /**
     * Parses a JSON string into an object of the specified type.
     * This method is useful for deserializing JSON into complex generic types.
     *
     * @param json The JSON string to parse
     * @param type The Type representing the target object structure
     * @param <T>  The type of the target object
     * @return An instance of type T parsed from the JSON string
     */
    public static <T> T fromJson(String json, Type type) {
        return gson.fromJson(json, type);
    }

    /**
     * Registers a Gson configurator with the specified key.
     * After registration, the configurator will be applied to rebuild the Gson instance.
     *
     * @param key          The key to identify the configurator
     * @param configurator The configurator to apply to GsonBuilder
     */
    public static void registerConfigurator(String key, Consumer<GsonBuilder> configurator) {
        configurators.put(key, configurator);
        applyConfigurators();
    }

    /**
     * Unregisters a Gson configurator by its key.
     * If the configurator exists and is removed, the Gson instance will be rebuilt.
     *
     * @param key The key of the configurator to unregister
     */
    public static void unregisterConfigurator(String key) {
        if (configurators.remove(key) != null) {
            applyConfigurators();
        }
    }

    /**
     * Applies all registered configurators to create a new Gson instance.
     */
    private static void applyConfigurators() {
        synchronized (lock) {
            GsonBuilder builder = new GsonBuilder();
            configurators.forEach((key, configurator) -> configurator.accept(builder));
            gson = builder.create();
        }
    }
}