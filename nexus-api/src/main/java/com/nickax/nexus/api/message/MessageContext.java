package com.nickax.nexus.api.message;

import org.jetbrains.annotations.NotNull;

/**
 * Context for a received message.
 */
public interface MessageContext {

    /**
     * Returns the node id that published the message.
     *
     * @return the source node id
     */
    @NotNull String sourceNodeId();

    /**
     * Returns whether this message was published by the local node.
     * Use this to skip self-published messages when a subscriber does not want
     * to react to its own publications.
     *
     * @return {@code true} if this node published the message
     */
    boolean fromSelf();
}
