package com.nickax.nexus.api.service;

import org.jetbrains.annotations.NotNull;

/**
 * Registers {@link Service}s against an owner key and drives their lifecycle.
 * Services registered under one owner are started in registration order and
 * stopped in reverse order when that owner is shut down.
 */
public interface ServiceManager {

    /**
     * Registers and immediately starts a service under the given owner.
     *
     * @param owner   the owner key (typically a plugin name)
     * @param service the service to register and start
     * @param <T>     the service type
     * @return the started service, for chaining
     * @throws RuntimeException if {@link Service#onStart()} fails
     */
    <T extends Service> @NotNull T register(@NotNull String owner, @NotNull T service);

    /**
     * Stops every service registered under the given owner, in reverse
     * registration order, and forgets them. Safe to call when the owner has
     * no services. Owners are responsible for calling this themselves on their own shutdown.
     *
     * @param owner the owner key whose services should be stopped
     */
    void stopAll(@NotNull String owner);
}
