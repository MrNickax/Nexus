package com.nickax.nexus.core.data;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.nickax.nexus.api.data.Codec;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MongoBackendTest {

    private static final Codec<String> CODEC = new Codec<>() {
        @Override public String encode(String value) { return value; }
        @Override public String decode(String data) { return data; }
    };
    private static final Executor DIRECT = Runnable::run;

    private static TransitionWalker.ReachedState<RunningMongodProcess> mongo;
    private static MongoClient client;
    private static int counter;

    private MongoBackend<String> backend;

    @BeforeAll
    static void startMongo() {
        mongo = Mongod.instance().start(Version.Main.V7_0);
        client = MongoClients.create("mongodb://" + mongo.current().getServerAddress());
    }

    @AfterAll
    static void stopMongo() {
        if (client != null) client.close();
        if (mongo != null) mongo.close();
    }

    @BeforeEach
    void freshCollection() {
        backend = new MongoBackend<>(
                client.getDatabase("test").getCollection("c" + (counter++)), CODEC, DIRECT);
    }

    @Test
    void put_thenGet_roundTrips() {
        backend.put("a", "1").join();
        assertEquals(Optional.of("1"), backend.get("a").join());
    }

    @Test
    void put_overwrites() {
        backend.put("a", "1").join();
        backend.put("a", "2").join();
        assertEquals(Optional.of("2"), backend.get("a").join());
    }

    @Test
    void get_absent_isEmpty() {
        assertEquals(Optional.empty(), backend.get("missing").join());
    }

    @Test
    void remove_deletesEntry() {
        backend.put("a", "1").join();
        backend.remove("a").join();
        assertFalse(backend.contains("a").join());
    }

    @Test
    void all_readsEveryEntry() {
        backend.put("a", "1").join();
        backend.put("b", "2").join();
        assertEquals(Map.of("a", "1", "b", "2"), backend.all().join());
    }
}
