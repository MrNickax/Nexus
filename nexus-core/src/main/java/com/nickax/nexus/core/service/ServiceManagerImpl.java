package com.nickax.nexus.core.service;

import com.nickax.nexus.api.service.Service;
import com.nickax.nexus.api.service.ServiceManager;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Default {@link ServiceManager}. Tracks one stack of services per owner so that
 * shutdown stops them in reverse registration order.
 */
public final class ServiceManagerImpl implements ServiceManager {

    private final Map<String, Deque<Service>> byOwner = new ConcurrentHashMap<>();
    private final System.Logger logger = System.getLogger("Nexus");

    /**
     * Starts the service and registers it under the given owner. If {@code onStart}
     * throws, the exception is wrapped and the service is not registered.
     *
     * @param owner   the owner identifier (typically the scope or plugin id)
     * @param service the service to start and track
     * @return the service itself (for chaining)
     */
    @Override
    public <T extends Service> @NotNull T register(@NotNull String owner, @NotNull T service) {
        try {
            service.onStart();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start service " + service.name(), e);
        }
        byOwner.computeIfAbsent(owner, k -> new ConcurrentLinkedDeque<>()).push(service);
        return service;
    }

    /**
     * Stops all services owned by the given owner in reverse registration order.
     * Stop exceptions are logged but do not prevent remaining services from stopping.
     *
     * @param owner the owner identifier
     */
    @Override
    public void stopAll(@NotNull String owner) {
        Deque<Service> services = byOwner.remove(owner);
        if (services == null) {
            return;
        }
        // push() = addFirst(); iterator() walks head->tail = reverse registration order
        for (Service service : services) {
            try {
                service.onStop();
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR,
                        "Failed to stop service " + service.name(), e);
            }
        }
    }
}
