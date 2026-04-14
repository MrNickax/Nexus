package com.nickax.nexus.common.storage;

import com.nickax.nexus.common.repository.BaseRepository;

/**
 * Abstract base class for storage implementations that provide asynchronous key-value storage operations.
 * <p>
 * This class serves as a foundation for concrete storage implementations by extending the {@link BaseRepository}
 * interface. Subclasses should provide specific implementations for the storage backend.
 *
 * @param <T> the type of values stored in this storage
 */
public abstract class Storage<T> implements BaseRepository<T> {
}