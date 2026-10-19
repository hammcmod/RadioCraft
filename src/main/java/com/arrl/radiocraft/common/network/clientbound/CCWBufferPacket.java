package com.arrl.radiocraft.common.network.clientbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioBlockEntity;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Morse received by the HF radio at {@code pos}, sent to every client tracking its chunk. The client queues the buffers
 * on the radio's {@link com.arrl.radiocraft.common.radio.morse.CWReceiveBuffer}, which the morse sound instance plays
 * back tick by tick at {@code strength}.
 *
 * @param pos The receiving radio.
 * @param buffers Key states to play, in any order.
 * @param strength Received signal strength, 0-1, used as the tone volume.
 */
public record CCWBufferPacket(BlockPos pos, List<CWBuffer> buffers, float strength) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CCWBufferPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("c_cw_buffer"));

    public static final StreamCodec<ByteBuf, CCWBufferPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            CCWBufferPacket::pos,
            CWBuffer.STREAM_CODEC.apply(ByteBufCodecs.list(64)),
            CCWBufferPacket::buffers,
            ByteBufCodecs.FLOAT,
            CCWBufferPacket::strength,
            CCWBufferPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final CCWBufferPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if(context.player().level().getBlockEntity(packet.pos()) instanceof HFRadioBlockEntity radio) {
                for(CWBuffer buffer : packet.buffers())
                    radio.getCWReceiveBuffer().addToBuffer(buffer, packet.strength());
            }
        });
    }

    /** Sends the buffers to everyone who can see the radio's chunk. */
    public static void sendToTracking(ServerLevel level, BlockPos pos, List<CWBuffer> buffers, float strength) {
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos), new CCWBufferPacket(pos, buffers, strength));
    }
}
