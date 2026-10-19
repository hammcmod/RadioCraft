package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioBlockEntity;
import com.arrl.radiocraft.common.menus.RadioMenu;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Morse key input recorded on the client ({@link com.arrl.radiocraft.common.radio.morse.CWSendBuffer}) for the HF radio
 * at {@code pos}. The server keys the radio's transmitter with it. Only accepted from the player who has that radio's
 * screen open, and only while the radio can transmit CW.
 *
 * @param pos The radio's position.
 * @param buffers The key states, in any order (they carry ids).
 */
public record SCWBufferPacket(BlockPos pos, List<CWBuffer> buffers) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SCWBufferPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_cw_buffer"));

    /** Clients send at most two buffers per packet; the cap keeps a bad client from flooding the transmitter. */
    public static final int MAX_BUFFERS = 8;

    public static final StreamCodec<ByteBuf, SCWBufferPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SCWBufferPacket::pos,
            CWBuffer.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_BUFFERS)),
            SCWBufferPacket::buffers,
            SCWBufferPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SCWBufferPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if(!(context.player().level().getBlockEntity(packet.pos()) instanceof HFRadioBlockEntity radio))
                return;
            if(!(context.player().containerMenu instanceof RadioMenu<?> menu) || menu.blockEntity != radio)
                return;
            if(radio.canTransmitCW())
                radio.transmitMorse(packet.buffers());
        });
    }
}
