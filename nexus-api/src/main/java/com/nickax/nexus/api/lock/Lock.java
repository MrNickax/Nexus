package com.nickax.nexus.api.lock;

/**
 * A held lock lease. Close it (e.g. via try-with-resources) to release.
 */
public interface Lock extends AutoCloseable {

    /**
     * Releases the lock. Never throws.
     */
    @Override
    void close();
}
