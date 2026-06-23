package com.nickax.nexus.core;

import com.nickax.nexus.api.Nexus;
import com.nickax.nexus.api.NexusScope;
import com.nickax.nexus.api.data.DataService;
import com.nickax.nexus.api.service.Service;
import org.jetbrains.annotations.NotNull;

/**
 * Default {@link NexusScope}: a scoped data service plus service registration
 * under the scope id as owner.
 */
final class NexusScopeImpl implements NexusScope {

    private final Nexus nexus;
    private final String id;
    private final DataService scopedData;

    /**
     * Creates a scope.
     *
     * @param nexus the owning Nexus instance
     * @param id    the scope identifier (must match {@code [A-Za-z0-9_]+})
     */
    NexusScopeImpl(@NotNull Nexus nexus, @NotNull String id) {
        this.nexus = nexus;
        this.id = id;
        this.scopedData = new ScopedDataService(nexus.data(), id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull String id() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull DataService data() {
        return scopedData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Service> @NotNull T register(@NotNull T service) {
        return nexus.services().register(id, service);
    }

    /**
     * Stops all services registered under this scope.
     */
    @Override
    public void close() {
        nexus.services().stopAll(id);
    }
}
