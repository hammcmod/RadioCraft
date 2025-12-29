package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SRadioPTTUpdatePacket(long pos, boolean ptt) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SRadioPTTUpdatePacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_radio_ptt_update"));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<ByteBuf, SRadioPTTUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            SRadioPTTUpdatePacket::pos,
            ByteBufCodecs.BOOL,
            SRadioPTTUpdatePacket::ptt,
            SRadioPTTUpdatePacket::new
    );

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockPos blockPos = BlockPos.of(pos);
            if(!(context.player().level().getBlockEntity(blockPos) instanceof RadioBlockEntity radio))
                return;

            radio.setPTTDown(ptt);
            if(ptt)
                radio.setMicPos(context.player().blockPosition());
            else
                radio.resetMicPos();
        });
    }

    public static void updateServer(BlockPos pos, boolean ptt) {
        PacketDistributor.sendToServer(new SRadioPTTUpdatePacket(pos.asLong(), ptt));
    }
}
