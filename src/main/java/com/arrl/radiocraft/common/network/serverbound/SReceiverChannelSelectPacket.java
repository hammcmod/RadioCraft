package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.radio.VHFReceiverBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SReceiverChannelSelectPacket(long pos, int channel) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SReceiverChannelSelectPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_receiver_channel_select"));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<ByteBuf, SReceiverChannelSelectPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            SReceiverChannelSelectPacket::pos,
            ByteBufCodecs.VAR_INT,
            SReceiverChannelSelectPacket::channel,
            SReceiverChannelSelectPacket::new
    );

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockPos blockPos = BlockPos.of(pos);
            if(!(context.player().level().getBlockEntity(blockPos) instanceof VHFReceiverBlockEntity radio))
                return;

            radio.selectChannel(channel);
        });
    }

    public static void updateServer(BlockPos pos, int channel) {
        PacketDistributor.sendToServer(new SReceiverChannelSelectPacket(pos.asLong(), channel));
    }
}
