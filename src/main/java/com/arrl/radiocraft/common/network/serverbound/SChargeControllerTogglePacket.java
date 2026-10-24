package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.menus.ChargeControllerMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** Flips a charge controller's switch from its screen. Only a player with that controller's screen open can. */
public record SChargeControllerTogglePacket(long pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SChargeControllerTogglePacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_charge_controller_toggle"));

    public static final StreamCodec<ByteBuf, SChargeControllerTogglePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, SChargeControllerTogglePacket::pos,
            SChargeControllerTogglePacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if(context.player().containerMenu instanceof ChargeControllerMenu menu && menu.blockEntity.getBlockPos().equals(BlockPos.of(pos)))
                menu.blockEntity.toggle();
        });
    }

    public static void send(BlockPos pos) {
        PacketDistributor.sendToServer(new SChargeControllerTogglePacket(pos.asLong()));
    }
}
