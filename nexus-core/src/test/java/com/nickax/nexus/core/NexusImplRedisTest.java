package com.nickax.nexus.core;

import com.nickax.nexus.api.data.DataStore;
import com.nickax.nexus.api.data.KeyMapper;
import com.nickax.nexus.api.data.RedisSettings;
import com.nickax.nexus.core.lock.LocalLockService;
import com.nickax.nexus.core.lock.RedissonLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NexusImplRedisTest {

    @Test
    void withoutRedis_usesLocalLockServiceAndDataWorks(@TempDir Path dir) {
        NexusImpl nexus = new NexusImpl(dir);
        try {
            assertInstanceOf(LocalLockService.class, nexus.locks());
            assertNotNull(nexus.data());
        } finally {
            nexus.shutdown();
        }
    }

    @Test
    void withRedis_usesRedissonLockServiceAndRedisCacheRoundTrips(@TempDir Path dir) throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        redis.embedded.RedisServer server = redis.embedded.RedisServer.newRedisServer().port(port).build();
        server.start();
        RedisSettings settings = new RedisSettings("127.0.0.1", port, null, 0);
        NexusImpl nexus = new NexusImpl(dir);
        try {
            assertInstanceOf(RedissonLockService.class, nexus.locks(settings));

            DataStore<UUID, String> store = nexus.data().<UUID, String>store("p3test_players", String.class)
                    .key(KeyMapper.uuid())
                    .redisCache(settings)
                    .build();
            UUID id = UUID.randomUUID();
            store.save(id, "Steve").join();
            assertEquals(Optional.of("Steve"), store.find(id).join());
        } finally {
            nexus.shutdown();
            server.stop();
        }
    }
}
