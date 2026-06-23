package com.nickax.nexus.core.message;

import com.nickax.nexus.api.message.MessageContext;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable {@link MessageContext}.
 *
 * @param sourceNodeId the publishing node id
 * @param fromSelf     whether the local node published it
 */
record SimpleMessageContext(@NotNull String sourceNodeId, boolean fromSelf) implements MessageContext {
}
