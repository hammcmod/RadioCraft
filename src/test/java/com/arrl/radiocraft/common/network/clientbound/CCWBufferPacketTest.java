package com.arrl.radiocraft.common.network.clientbound;

import com.arrl.radiocraft.common.network.serverbound.SCWBufferPacket;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Both morse packets carry a position and a list of {@link CWBuffer}s; the clientbound one adds the strength.
 */
class CCWBufferPacketTest {

    private static final BlockPos POS = new BlockPos(-120, 64, 3_000_000);

    private static List<CWBuffer> buffers() {
        return List.of(new CWBuffer(3, new boolean[] { true, false, true }), new CWBuffer(4, new boolean[CWBuffer.BUFFER_LENGTH]));
    }

    private static void assertSameBuffers(List<CWBuffer> expected, List<CWBuffer> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).getId(), actual.get(i).getId());
            assertArrayEquals(expected.get(i).getInputs(), actual.get(i).getInputs());
        }
    }

    @Test
    void clientboundRoundTrips() {
        ByteBuf buf = Unpooled.buffer();
        CCWBufferPacket.STREAM_CODEC.encode(buf, new CCWBufferPacket(POS, buffers(), 0.73F));
        CCWBufferPacket decoded = CCWBufferPacket.STREAM_CODEC.decode(buf);
        assertEquals(POS, decoded.pos());
        assertSameBuffers(buffers(), decoded.buffers());
        assertEquals(0.73F, decoded.strength(), 0.0F);
        assertEquals(0, buf.readableBytes(), "Packet left unread bytes");
    }

    @Test
    void serverboundRoundTrips() {
        ByteBuf buf = Unpooled.buffer();
        SCWBufferPacket.STREAM_CODEC.encode(buf, new SCWBufferPacket(POS, buffers()));
        SCWBufferPacket decoded = SCWBufferPacket.STREAM_CODEC.decode(buf);
        assertEquals(POS, decoded.pos());
        assertSameBuffers(buffers(), decoded.buffers());
        assertEquals(0, buf.readableBytes(), "Packet left unread bytes");
    }

    @Test
    void serverboundRejectsFloods() {
        ByteBuf buf = Unpooled.buffer();
        List<CWBuffer> tooMany = IntStream.range(0, SCWBufferPacket.MAX_BUFFERS + 1)
                .mapToObj(i -> new CWBuffer(i, new boolean[] { true })).toList();
        assertThrows(Exception.class, () -> {
            SCWBufferPacket.STREAM_CODEC.encode(buf, new SCWBufferPacket(POS, tooMany));
            SCWBufferPacket.STREAM_CODEC.decode(buf);
        });
    }

    @Test
    void typeIdsAreInTheModNamespace() {
        assertEquals("radiocraft", CCWBufferPacket.TYPE.id().getNamespace());
        assertEquals("radiocraft", SCWBufferPacket.TYPE.id().getNamespace());
    }
}
