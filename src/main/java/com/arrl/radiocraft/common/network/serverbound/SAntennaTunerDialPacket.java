package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.menus.AntennaTunerMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** Turns one of a manual antenna tuner's dials from its screen. Only a player with that tuner's screen open can. */
public record SAntennaTunerDialPacket(long pos, int dial, int value) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SAntennaTunerDialPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_antenna_tuner_dial"));

    public static final StreamCodec<ByteBuf, SAntennaTunerDialPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, SAntennaTunerDialPacket::pos,
            ByteBufCodecs.VAR_INT, SAntennaTunerDialPacket::dial,
            ByteBufCodecs.VAR_INT, SAntennaTunerDialPacket::value,
            SAntennaTunerDialPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if(context.player().containerMenu instanceof AntennaTunerMenu menu && menu.blockEntity.getBlockPos().equals(BlockPos.of(pos)))
                menu.blockEntity.setDial(dial, value);
        });
    }

    public static void send(BlockPos pos, int dial, int value) {
        PacketDistributor.sendToServer(new SAntennaTunerDialPacket(pos.asLong(), dial, value));
    }
}
