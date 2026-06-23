package com.nickax.nexus.core.data;

import com.nickax.nexus.api.data.DataService;
import com.nickax.nexus.api.data.DataStore;
import com.nickax.nexus.api.data.DataStoreBuilder;
import com.nickax.nexus.api.lock.LockService;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Default {@link DataService}. Hands builders the shared executor, the local lock
 * service, the connection registry (for resolving Redis/Mongo/SQL clients by
 * settings), the data flush scheduler, and a tracking hook so all built stores can
 * be flushed on shutdown.
 */
public final class DataServiceImpl implements DataService {

    private final Path dataRoot;
    private final Executor executor;
    private final LockService localLockService;
    private final ConnectionRegistry connections;
    private final ScheduledExecutorService dataScheduler;
    private final List<DataStore<?, ?>> stores = new CopyOnWriteArrayList<>();

    /**
     * Creates the data service.
     *
     * @param dataFolder       the Nexus base data folder
     * @param executor         the shared executor
     * @param localLockService the local lock service (used when a store has no Redis)
     * @param connections      the shared connection registry
     * @param dataScheduler    the scheduler for periodic write-behind flushes
     */
    public DataServiceImpl(@NotNull Path dataFolder, @NotNull Executor executor, @NotNull LockService localLockService, @NotNull ConnectionRegistry connections, @NotNull ScheduledExecutorService dataScheduler) {
        this.dataRoot = dataFolder.resolve("data");
        this.executor = executor;
        this.localLockService = localLockService;
        this.connections = connections;
        this.dataScheduler = dataScheduler;
    }

    /**
     * Returns a builder for a new {@link com.nickax.nexus.api.data.DataStore}. The
     * built store is registered with the internal tracker so it is included in
     * {@link #flushAll()}.
     *
     * @param name the store name (must match {@code [A-Za-z0-9_]+})
     * @param type the value class
     * @return a configured builder
     */
    @Override
    public <K, V> @NotNull DataStoreBuilder<K, V> store(@NotNull String name, @NotNull Class<V> type) {
        return new DataStoreBuilderImpl<>(name, type, dataRoot, executor, localLockService, connections, dataScheduler, stores::add);
    }

    /**
     * Flushes every store built through this service, blocking until done. Called on
     * shutdown so write-behind data is persisted before the executors stop.
     *
     * <p>Stores are tracked for the lifetime of this service and not untracked
     * individually; on a server that reloads plugins many times this list grows
     * until restart.
     */
    public void flushAll() {
        for (DataStore<?, ?> store : stores) {
            try {
                store.flush().join();
            } catch (RuntimeException e) {
                System.getLogger("Nexus").log(System.Logger.Level.ERROR,
                        "Failed to flush a store on shutdown; its write-behind data may be lost", e);
            }
        }
    }
}
