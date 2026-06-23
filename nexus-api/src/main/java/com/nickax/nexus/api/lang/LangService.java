package com.nickax.nexus.api.lang;

import org.jetbrains.annotations.NotNull;

/**
 * Entry point for building {@link Lang} instances. Obtain via {@code nexus.lang()}.
 */
public interface LangService {

    /**
     * Returns a new builder for constructing a {@link Lang} instance.
     *
     * @return a new Lang builder
     */
    @NotNull LangBuilder builder();
}
