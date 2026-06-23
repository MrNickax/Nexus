package com.nickax.nexus.core.lang;

import com.nickax.nexus.api.lang.LangBuilder;
import com.nickax.nexus.api.lang.LangService;
import org.jetbrains.annotations.NotNull;

/**
 * Default {@link LangService}.
 */
public final class LangServiceImpl implements LangService {

    /**
     * Returns a fresh builder for constructing a {@link com.nickax.nexus.api.lang.Lang}
     * instance.
     *
     * @return a new {@link LangBuilderImpl}
     */
    @Override
    public @NotNull LangBuilder builder() {
        return new LangBuilderImpl();
    }
}
