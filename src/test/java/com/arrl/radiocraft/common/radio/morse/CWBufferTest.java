package com.arrl.radiocraft.common.radio.morse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Morse key states travel as a bitmask; make sure nothing is lost or reordered on the way.
 */
class CWBufferTest {

    private static boolean[] pattern() {
        boolean[] inputs = new boolean[CWBuffer.BUFFER_LENGTH];
        for (int i = 0; i < inputs.length; i++)
            inputs[i] = i < 3 || (i >= 6 && i < 15) || i == 19;
        return inputs;
    }

    @Test
    void maskRoundTripsEveryTick() {
        CWBuffer original = new CWBuffer(7, pattern());
        CWBuffer copy = CWBuffer.fromMask(original.getId(), original.toMask());
        assertEquals(7, copy.getId());
        assertArrayEquals(original.getInputs(), copy.getInputs());
    }

    @Test
    void codecRoundTrips() {
        ByteBuf buf = Unpooled.buffer();
        CWBuffer.STREAM_CODEC.encode(buf, new CWBuffer(42, pattern()));
        CWBuffer decoded = CWBuffer.STREAM_CODEC.decode(buf);
        assertEquals(42, decoded.getId());
        assertArrayEquals(pattern(), decoded.getInputs());
        assertEquals(0, buf.readableBytes(), "Packet left unread bytes");
    }

    @Test
    void shortInputIsPaddedWithKeyUp() {
        CWBuffer buffer = new CWBuffer(0, new boolean[] { true, true });
        assertEquals(CWBuffer.BUFFER_LENGTH, buffer.getInputs().length);
        assertTrue(buffer.hasKeyDown());
        assertEquals(0b11, buffer.toMask());
    }

    @Test
    void emptyBufferHasNoKeyDown() {
        assertFalse(new CWBuffer(0, new boolean[CWBuffer.BUFFER_LENGTH]).hasKeyDown());
    }

    @Test
    void readNextWalksEveryTickThenFinishes() {
        CWBuffer buffer = new CWBuffer(0, pattern());
        boolean[] expected = pattern();
        for (int i = 0; i < CWBuffer.BUFFER_LENGTH; i++) {
            assertFalse(buffer.isFinished());
            assertEquals(expected[i], buffer.readNext(), "tick " + i);
        }
        assertTrue(buffer.isFinished());
    }

    @Test
    void receiveBufferOrdersByIdAndPlaysStrength() {
        CWReceiveBuffer receive = new CWReceiveBuffer();
        receive.addToBuffer(new CWBuffer(1, new boolean[] { false, true }), 0.5F);
        receive.addToBuffer(new CWBuffer(0, new boolean[] { true }), 0.9F);
        assertEquals(0.9F, receive.getNextStrength(), 0.0F, "buffer 0, tick 0 is key-down");
        for (int i = 1; i < CWBuffer.BUFFER_LENGTH; i++)
            assertEquals(0.0F, receive.getNextStrength(), 0.0F, "buffer 0, tick " + i);
        assertEquals(0.0F, receive.getNextStrength(), 0.0F, "buffer 1, tick 0");
        assertEquals(0.5F, receive.getNextStrength(), 0.0F, "buffer 1, tick 1 plays at its own strength");
    }
}
