package com.nickax.nexus.core.data;

import com.nickax.nexus.api.data.Codec;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GsonCodecTest {

    static final class Point {
        int x;
        int y;
        Point() { }
        Point(int x, int y) { this.x = x; this.y = y; }
        @Override public boolean equals(Object o) {
            return o instanceof Point p && p.x == x && p.y == y;
        }
        @Override public int hashCode() { return Objects.hash(x, y); }
    }

    @Test
    void encodeThenDecode_roundTrips() {
        Codec<Point> codec = new GsonCodec<>(Point.class);
        String encoded = codec.encode(new Point(3, 4));
        assertEquals(new Point(3, 4), codec.decode(encoded));
    }

    @Test
    void encode_producesJson() {
        Codec<Point> codec = new GsonCodec<>(Point.class);
        assertEquals("{\"x\":1,\"y\":2}", codec.encode(new Point(1, 2)));
    }
}
