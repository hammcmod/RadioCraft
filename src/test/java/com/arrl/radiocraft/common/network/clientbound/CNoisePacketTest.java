package com.arrl.radiocraft.common.network.clientbound;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The solar-noise packet is a single float; make sure it survives the wire unchanged.
 */
class CNoisePacketTest {

    @Test
    void noiseRoundTripsThroughTheCodec() {
        for (float noise : new float[] { 0.0F, 0.1F, 0.3F, 0.7F, 1.0F }) {
            ByteBuf buf = Unpooled.buffer();
            CNoisePacket.STREAM_CODEC.encode(buf, new CNoisePacket(noise));
            CNoisePacket decoded = CNoisePacket.STREAM_CODEC.decode(buf);
            assertEquals(noise, decoded.noise(), 0.0F);
            assertEquals(0, buf.readableBytes(), "Packet left unread bytes");
        }
    }

    @Test
    void typeIdIsInTheModNamespace() {
        assertEquals("radiocraft", CNoisePacket.TYPE.id().getNamespace());
    }
}
