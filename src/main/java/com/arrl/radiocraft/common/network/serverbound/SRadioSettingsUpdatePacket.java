package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.radio.VHFBaseStationBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SRadioSettingsUpdatePacket(long pos, float frequencyHertz, float gain, float micGain) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SRadioSettingsUpdatePacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_radio_settings_update"));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<ByteBuf, SRadioSettingsUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            SRadioSettingsUpdatePacket::pos,
            ByteBufCodecs.FLOAT,
            SRadioSettingsUpdatePacket::frequencyHertz,
            ByteBufCodecs.FLOAT,
            SRadioSettingsUpdatePacket::gain,
            ByteBufCodecs.FLOAT,
            SRadioSettingsUpdatePacket::micGain,
            SRadioSettingsUpdatePacket::new
    );

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockPos blockPos = BlockPos.of(pos);
            if(!(context.player().level().getBlockEntity(blockPos) instanceof VHFBaseStationBlockEntity radio))
                return;

            radio.applySettings(frequencyHertz, gain, micGain);
        });
    }

    public static void updateServer(BlockPos pos, float frequencyHertz, float gain, float micGain) {
        PacketDistributor.sendToServer(new SRadioSettingsUpdatePacket(pos.asLong(), frequencyHertz, gain, micGain));
    }
}
