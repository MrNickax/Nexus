package com.nickax.nexus.core;

import com.nickax.nexus.api.Nexus;
import com.nickax.nexus.api.NexusProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NexusProviderTest {

    @AfterEach
    void clearProvider() {
        NexusProvider.set(null);
    }

    @Test
    void get_throwsWhenNotLoaded() {
        assertFalse(NexusProvider.isLoaded());
        assertThrows(IllegalStateException.class, NexusProvider::get);
    }

    @Test
    void get_returnsPublishedInstance(@TempDir Path dir) {
        Nexus nexus = new NexusImpl(dir);
        NexusProvider.set(nexus);

        assertTrue(NexusProvider.isLoaded());
        assertSame(nexus, NexusProvider.get());
    }
}
