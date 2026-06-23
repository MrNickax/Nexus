package com.nickax.nexus.core;

import com.nickax.nexus.api.Nexus;
import com.nickax.nexus.api.NexusScope;
import com.nickax.nexus.api.config.ConfigService;
import com.nickax.nexus.api.data.DataService;
import com.nickax.nexus.api.data.RedisSettings;
import com.nickax.nexus.api.lang.LangService;
import com.nickax.nexus.api.lock.LockService;
import com.nickax.nexus.api.message.Messaging;
import com.nickax.nexus.api.schedule.Scheduler;
import com.nickax.nexus.api.service.ServiceManager;
import com.nickax.nexus.api.webhook.WebhookService;
import com.nickax.nexus.core.config.ConfigServiceImpl;
import com.nickax.nexus.core.data.ConnectionRegistry;
import com.nickax.nexus.core.data.DataServiceImpl;
import com.nickax.nexus.core.lang.LangServiceImpl;
import com.nickax.nexus.core.lock.LocalLockService;
import com.nickax.nexus.core.lock.RedissonLockService;
import com.nickax.nexus.core.message.RedissonMessaging;
import com.nickax.nexus.core.service.ServiceManagerImpl;
import com.nickax.nexus.core.webhook.WebhookServiceImpl;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Base, platform-agnostic {@link Nexus} implementation. Owns the shared
 * virtual-thread executor, the data flush scheduler, the local lock service, the
 * config/lang/webhook services, and a {@link ConnectionRegistry} that shares
 * backend clients across plugins. The node id is generated once and persisted
 * under the data folder so messaging can ignore its own messages without any
 * user-facing configuration.
 */
public class NexusImpl implements Nexus {

    private static final java.util.regex.Pattern VALID_SCOPE_ID = java.util.regex.Pattern.compile("[A-Za-z0-9_]+");

    private final Path dataFolder;
    private final String nodeId;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ScheduledExecutorService dataScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "nexus-data-flush");
        thread.setDaemon(true);
        return thread;
    });
    private final ServiceManager serviceManager = new ServiceManagerImpl();
    private final LocalLockService localLockService = new LocalLockService();
    private final ConnectionRegistry connections = new ConnectionRegistry();
    private final DataService dataService;
    private final ConfigService configService = new ConfigServiceImpl();
    private final LangService langService = new LangServiceImpl();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final WebhookService webhookService = new WebhookServiceImpl(httpClient);
    private final Map<RedisSettings, RedissonLockService> redisLocks = new ConcurrentHashMap<>();
    private final Map<RedisSettings, Messaging> messagingByRedis = new ConcurrentHashMap<>();
    private final Map<String, NexusScopeImpl> scopes = new ConcurrentHashMap<>();
    private Scheduler scheduler;
    private volatile boolean closed;

    /**
     * Creates a Nexus instance rooted at a data folder.
     *
     * @param dataFolder the base folder for file-backed stores and the persisted node id
     */
    public NexusImpl(@NotNull Path dataFolder) {
        this.dataFolder = dataFolder;
        this.nodeId = loadOrCreateNodeId(dataFolder);
        this.dataService = new DataServiceImpl(dataFolder, executor, localLockService, connections, dataScheduler);
    }

    /**
     * Reads the persisted node id, generating and storing one on first run.
     *
     * @param dataFolder the data folder
     * @return a stable, server-unique node id
     */
    private String loadOrCreateNodeId(Path dataFolder) {
        Path file = dataFolder.resolve("node-id");
        try {
            if (Files.exists(file)) {
                return Files.readString(file).trim();
            }
            Files.createDirectories(dataFolder);
            String id = UUID.randomUUID().toString().substring(0, 8);
            Files.writeString(file, id);
            return id;
        } catch (IOException e) {
            String ephemeral = UUID.randomUUID().toString().substring(0, 8);
            System.getLogger("Nexus").log(System.Logger.Level.WARNING,
                    "Failed to persist node-id under " + dataFolder
                            + "; using an ephemeral id (" + ephemeral + "). Cross-server messaging"
                            + " deduplication may misfire across restarts.", e);
            return ephemeral;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull ServiceManager services() {
        return serviceManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull String nodeId() {
        return nodeId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull DataService data() {
        return dataService;
    }

    /**
     * Returns the in-JVM {@link LockService} backed by per-key semaphores.
     *
     * @return the local lock service
     */
    @Override
    public @NotNull LockService locks() {
        return localLockService;
    }

    /**
     * Returns a distributed {@link LockService} over Redis, sharing one
     * {@link RedissonLockService} per distinct settings object. Throws if
     * {@link #shutdown()} has already been called.
     *
     * @param settings the Redis connection settings
     * @return the distributed lock service for those settings
     * @throws IllegalStateException if this Nexus instance is shut down
     */
    @Override
    public @NotNull LockService locks(@NotNull RedisSettings settings) {
        if (closed) {
            throw new IllegalStateException("Nexus has been shut down");
        }
        return redisLocks.computeIfAbsent(settings, s -> new RedissonLockService(connections.redisson(s)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull ConfigService configs() {
        return configService;
    }

    /**
     * Returns the {@link Messaging} instance for the given Redis connection, sharing
     * one {@link RedissonMessaging} per distinct settings object. Throws if
     * {@link #shutdown()} has already been called.
     *
     * @param settings the Redis connection settings
     * @return the messaging instance for those settings
     * @throws IllegalStateException if this Nexus instance is shut down
     */
    @Override
    public @NotNull Messaging messaging(@NotNull RedisSettings settings) {
        if (closed) {
            throw new IllegalStateException("Nexus has been shut down");
        }
        return messagingByRedis.computeIfAbsent(settings,
                s -> new RedissonMessaging(connections.redisson(s), nodeId, executor));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull WebhookService webhooks() {
        return webhookService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull LangService lang() {
        return langService;
    }

    /**
     * Returns the scope for the given id, creating it on first access. The scope id
     * must match {@code [A-Za-z0-9_]+}; this constraint ensures it is safe as a
     * store name prefix and as a service owner identifier.
     *
     * @param id the scope identifier
     * @return the scope
     * @throws IllegalArgumentException if the id contains illegal characters
     */
    @Override
    public @NotNull NexusScope scope(@NotNull String id) {
        if (!VALID_SCOPE_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Scope id must match [A-Za-z0-9_]+ (got '" + id + "')");
        }
        return scopes.computeIfAbsent(id, key -> new NexusScopeImpl(this, key));
    }

    /**
     * The connection registry used by the data builder to resolve shared clients.
     *
     * @return the connection registry
     */
    public @NotNull ConnectionRegistry connections() {
        return connections;
    }

    /**
     * The base folder for file-backed stores.
     *
     * @return the data folder
     */
    public @NotNull Path dataFolder() {
        return dataFolder;
    }

    /**
     * Installs the platform scheduler. Called once by the platform module's hub.
     *
     * @param scheduler the platform scheduler
     */
    protected void installScheduler(@NotNull Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Returns the platform scheduler. Throws if the platform module has not yet
     * called {@link #installScheduler}.
     *
     * @return the installed scheduler
     * @throws IllegalStateException if no scheduler has been installed
     */
    @Override
    public @NotNull Scheduler scheduler() {
        if (scheduler == null) {
            throw new IllegalStateException("No scheduler installed on this platform");
        }
        return scheduler;
    }

    /**
     * Shuts down Nexus-owned infrastructure: flushes data stores, then stops the
     * schedulers and executor, closes the shared connections, and closes the HTTP client.
     */
    public void shutdown() {
        ((DataServiceImpl) dataService).flushAll();
        dataScheduler.shutdown();
        try {
            if (!dataScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                dataScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            dataScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        connections.closeAll();
        httpClient.close();
        closed = true;
    }
}
