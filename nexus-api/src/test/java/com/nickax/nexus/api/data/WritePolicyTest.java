package com.nickax.nexus.api.data;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class WritePolicyTest {

    @Test
    void writeThrough_isWriteThrough() {
        assertInstanceOf(WritePolicy.WriteThrough.class, WritePolicy.writeThrough());
    }

    @Test
    void writeBehind_carriesInterval() {
        WritePolicy policy = WritePolicy.writeBehind(Duration.ofSeconds(30));
        WritePolicy.WriteBehind behind = assertInstanceOf(WritePolicy.WriteBehind.class, policy);
        assertEquals(Duration.ofSeconds(30), behind.interval());
    }
}
