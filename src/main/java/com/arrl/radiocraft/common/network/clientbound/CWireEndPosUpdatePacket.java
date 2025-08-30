package com.arrl.radiocraft.common.network.clientbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.entities.AntennaWire;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record CWireEndPosUpdatePacket(int id, long endPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CWireEndPosUpdatePacket> TYPE = new CustomPacketPayload.Type<CWireEndPosUpdatePacket>(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "wire_end_pos"));

    public static final StreamCodec<ByteBuf, CWireEndPosUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CWireEndPosUpdatePacket::id,
            ByteBufCodecs.VAR_LONG,
            CWireEndPosUpdatePacket::endPos,
            CWireEndPosUpdatePacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final CWireEndPosUpdatePacket data, final IPayloadContext context) {
        // Do something with the data, on the main thread
        context.enqueueWork(() -> {
            Radiocraft.LOGGER.info("Received CWireEndPosUpdatePacket for entity {} at {}", context.player().getName(), data.endPos());
            try (Level level = context.player().level()) {
                Entity e = level.getEntity(data.id);
                if (e instanceof AntennaWire) {
                    ((AntennaWire) e).setEndPos(BlockPos.of(data.endPos()));
                } else {
                    if (e != null)
                        Radiocraft.LOGGER.error("Received CWireEndPosUpdatePacket for entity {} which is not an AntennaWire", e.getName());
                }
            } catch (Exception e) {
                Radiocraft.LOGGER.error("Error handling CWireEndPosUpdatePacket", e);
            }
        });
    }
}
