package com.nickax.nexus.api;

import com.nickax.nexus.api.config.ConfigService;
import com.nickax.nexus.api.data.DataService;
import com.nickax.nexus.api.data.RedisSettings;
import com.nickax.nexus.api.lang.LangService;
import com.nickax.nexus.api.lock.LockService;
import com.nickax.nexus.api.message.Messaging;
import com.nickax.nexus.api.schedule.Scheduler;
import com.nickax.nexus.api.service.ServiceManager;
import com.nickax.nexus.api.webhook.WebhookService;
import org.jetbrains.annotations.NotNull;

/**
 * The Nexus hub: the single entry point to every platform-agnostic sub-API.
 * Obtain the active instance via {@link NexusProvider#get()}. Additional
 * accessors (data, scheduler, messaging, lang, configs, locks, listeners,
 * webhooks) are introduced by their respective subsystems in later plans.
 */
public interface Nexus {

    /**
     * The service manager that drives plugin-scoped lifecycle.
     *
     * @return the service manager, never {@code null}
     */
    @NotNull ServiceManager services();

    /**
     * A short identifier for the server/proxy node, used by messaging to ignore
     * its own messages. Generated once and persisted under the data folder.
     *
     * @return the node id, never {@code null}
     */
    @NotNull String nodeId();

    /**
     * The data service: a factory for cache-backed data stores.
     *
     * @return the data service, never {@code null}
     */
    @NotNull DataService data();

    /**
     * The local lock service for named mutual-exclusion locks (no Redis required).
     *
     * @return the local lock service, never {@code null}
     */
    @NotNull LockService locks();

    /**
     * A distributed lock service backed by Redis. The underlying Redisson client
     * is shared with any other consumer that passes the same settings.
     *
     * @param settings the Redis connection settings
     * @return the distributed lock service, never {@code null}
     */
    @NotNull LockService locks(@NotNull RedisSettings settings);

    /**
     * The config service: loads YAML configuration files.
     *
     * @return the config service, never {@code null}
     */
    @NotNull ConfigService configs();

    /**
     * The cross-server messaging service backed by Redis. The underlying Redisson
     * client is shared with any other consumer that passes the same settings.
     *
     * @param settings the Redis connection settings
     * @return the messaging service, never {@code null}
     */
    @NotNull Messaging messaging(@NotNull RedisSettings settings);

    /**
     * The webhook service for sending Discord webhook messages.
     *
     * @return the webhook service, never {@code null}
     */
    @NotNull WebhookService webhooks();

    /**
     * The i18n service for building localized message renderers.
     *
     * @return the lang service, never {@code null}
     */
    @NotNull LangService lang();

    /**
     * The platform task scheduler (Bukkit/Folia on the server, the proxy scheduler
     * on a proxy).
     *
     * @return the scheduler
     * @throws IllegalStateException if no scheduler is installed (non-platform context)
     */
    @NotNull Scheduler scheduler();

    /**
     * Returns a per-plugin scope. Data stores are namespaced with {@code id} and
     * services registered through the scope are stopped together on
     * {@link NexusScope#close()}.
     *
     * <p><b>Collision note:</b> a scoped store's global name is {@code id + "_" + storeName}.
     * Because both the id and the store name may contain underscores, two different
     * (id, storeName) pairs can in principle map to the same global name (e.g. id {@code "a"}
     * with store {@code "b_c"} equals id {@code "a_b"} with store {@code "c"}). Use distinct
     * scope ids (typically plugin names) and this never occurs in practice.
     *
     * @param id the scope id; must match {@code [A-Za-z0-9_]+} (e.g. a plugin name)
     * @return a scope bound to the id
     */
    @NotNull NexusScope scope(@NotNull String id);
}
