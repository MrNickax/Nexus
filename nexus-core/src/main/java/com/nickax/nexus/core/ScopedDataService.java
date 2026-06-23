package com.nickax.nexus.core;

import com.nickax.nexus.api.data.DataService;
import com.nickax.nexus.api.data.DataStoreBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link DataService} that prefixes every store name with a scope id, so a
 * plugin's {@code store("accounts")} becomes the global store {@code <id>_accounts}.
 */
final class ScopedDataService implements DataService {

    private final DataService delegate;
    private final String id;

    /**
     * Creates a scoped data service.
     *
     * @param delegate the global data service that performs the actual store creation
     * @param id       the scope id used as the name prefix
     */
    ScopedDataService(@NotNull DataService delegate, @NotNull String id) {
        this.delegate = delegate;
        this.id = id;
    }

    /**
     * Returns a builder for a store whose name is prefixed with this scope id.
     *
     * @param name the local store name
     * @param type the value class
     * @return a builder for the globally-named store {@code <id>_<name>}
     */
    @Override
    public <K, V> @NotNull DataStoreBuilder<K, V> store(@NotNull String name, @NotNull Class<V> type) {
        return delegate.store(id + "_" + name, type);
    }
}
