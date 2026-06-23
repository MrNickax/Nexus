package com.nickax.nexus.api.service;

/**
 * A lifecycle unit managed by Nexus. A service is started once and stopped once.
 * Services are registered against an owner (a plugin) and are stopped in reverse
 * registration order when that owner shuts down.
 */
public interface Service {

    /**
     * Starts this service. Called once, on the owner's startup.
     *
     * @throws Exception if startup fails; the owner's bootstrap should abort
     */
    void onStart() throws Exception;

    /**
     * Stops this service, releasing any resources it holds. Called once, on the
     * owner's shutdown. Implementations must not throw; failures should be logged.
     */
    void onStop();

    /**
     * A short, stable name for this service, used in logs.
     *
     * @return the service name; defaults to the simple class name
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
