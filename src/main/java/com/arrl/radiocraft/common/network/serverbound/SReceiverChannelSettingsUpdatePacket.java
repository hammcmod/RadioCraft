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

public record SReceiverChannelSettingsUpdatePacket(long pos, int channel, float frequencyHertz, float gain) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SReceiverChannelSettingsUpdatePacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_receiver_channel_settings_update"));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<ByteBuf, SReceiverChannelSettingsUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            SReceiverChannelSettingsUpdatePacket::pos,
            ByteBufCodecs.VAR_INT,
            SReceiverChannelSettingsUpdatePacket::channel,
            ByteBufCodecs.FLOAT,
            SReceiverChannelSettingsUpdatePacket::frequencyHertz,
            ByteBufCodecs.FLOAT,
            SReceiverChannelSettingsUpdatePacket::gain,
            SReceiverChannelSettingsUpdatePacket::new
    );

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockPos blockPos = BlockPos.of(pos);
            if(!(context.player().level().getBlockEntity(blockPos) instanceof VHFReceiverBlockEntity radio))
                return;

            radio.applyChannelSettings(channel, frequencyHertz, gain);
        });
    }

    public static void updateServer(BlockPos pos, int channel, float frequencyHertz, float gain) {
        PacketDistributor.sendToServer(new SReceiverChannelSettingsUpdatePacket(pos.asLong(), channel, frequencyHertz, gain));
    }
}
