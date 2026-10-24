package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.menus.DuplexerMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** Turns one of a duplexer's cavity screws to a frequency from its screen. Only a player with that duplexer's screen open can. */
public record SDuplexerScrewPacket(long pos, int screw, int hz) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SDuplexerScrewPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_duplexer_screw"));

    public static final StreamCodec<ByteBuf, SDuplexerScrewPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, SDuplexerScrewPacket::pos,
            ByteBufCodecs.VAR_INT, SDuplexerScrewPacket::screw,
            ByteBufCodecs.VAR_INT, SDuplexerScrewPacket::hz,
            SDuplexerScrewPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if(context.player().containerMenu instanceof DuplexerMenu menu && menu.blockEntity.getBlockPos().equals(BlockPos.of(pos)))
                menu.blockEntity.setScrewHz(screw, hz);
        });
    }

    public static void send(BlockPos pos, int screw, int hz) {
        PacketDistributor.sendToServer(new SDuplexerScrewPacket(pos.asLong(), screw, hz));
    }
}
