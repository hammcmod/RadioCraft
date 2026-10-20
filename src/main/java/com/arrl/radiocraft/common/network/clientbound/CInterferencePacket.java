package com.arrl.radiocraft.common.network.clientbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
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

/**
 * One tick's worth of redstone interference picked up by the radio at {@code pos}, sent to every client tracking its
 * chunk. The client adds it to its own copy of the radio's {@link com.arrl.radiocraft.common.radio.rfi.InterferenceLevel},
 * which decays on its own, so only the pops travel and a quiet radio sends nothing.
 *
 * @param pos The radio.
 * @param amount Noise added this tick, 0 to 1.
 */
public record CInterferencePacket(BlockPos pos, float amount) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CInterferencePacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("c_interference"));

    public static final StreamCodec<ByteBuf, CInterferencePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            CInterferencePacket::pos,
            ByteBufCodecs.FLOAT,
            CInterferencePacket::amount,
            CInterferencePacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final CInterferencePacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if(context.player().level().getBlockEntity(packet.pos()) instanceof RadioBlockEntity radio)
                radio.getInterferenceLevel().add(context.player().level().getGameTime(), packet.amount());
        });
    }

    public static void sendToTracking(ServerLevel level, BlockPos pos, double amount) {
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos), new CInterferencePacket(pos, (float)amount));
    }
}
