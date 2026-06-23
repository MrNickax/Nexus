package com.nickax.nexus.core;

import com.nickax.nexus.api.data.DataStore;
import com.nickax.nexus.api.data.KeyMapper;
import com.nickax.nexus.api.data.SqlSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NexusImplBackendsTest {

    @Test
    void sqlBackend_endToEndThroughHikari(@TempDir Path dir) {
        String url = "jdbc:h2:mem:e2e" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        NexusImpl nexus = new NexusImpl(dir);
        try {
            DataStore<String, String> store = nexus.data().<String, String>store("e2e_players", String.class)
                    .key(KeyMapper.string())
                    .sqlBackend(new SqlSettings(url, "sa", null, 2))
                    .build();
            store.save("a", "Steve").join();
            assertEquals(Optional.of("Steve"), store.find("a").join());
        } finally {
            nexus.shutdown();
        }
    }
}
