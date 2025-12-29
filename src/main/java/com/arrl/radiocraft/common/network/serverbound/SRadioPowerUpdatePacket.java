package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SRadioPowerUpdatePacket(long pos, boolean powered) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SRadioPowerUpdatePacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_radio_power_update"));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<ByteBuf, SRadioPowerUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            SRadioPowerUpdatePacket::pos,
            ByteBufCodecs.BOOL,
            SRadioPowerUpdatePacket::powered,
            SRadioPowerUpdatePacket::new
    );

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockPos blockPos = BlockPos.of(pos);
            if(!(context.player().level().getBlockEntity(blockPos) instanceof RadioBlockEntity radio))
                return;

            if(radio.getNetworkObject(context.player().level(), blockPos) instanceof RadioNetworkObject networkObject) {
                networkObject.isPowered = powered;
                radio.updateIsReceiving();
                radio.syncToClient();
            }
        });
    }

    public static void updateServer(BlockPos pos, boolean powered) {
        PacketDistributor.sendToServer(new SRadioPowerUpdatePacket(pos.asLong(), powered));
    }
}
