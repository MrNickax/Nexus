package com.nickax.nexus.api.message;

/**
 * A handle for an active channel subscription. Close it to stop receiving.
 */
public interface Subscription extends AutoCloseable {

    /**
     * Cancels the subscription.
     */
    @Override
    void close();
}
