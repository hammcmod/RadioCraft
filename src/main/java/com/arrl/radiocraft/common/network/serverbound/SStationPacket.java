package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.menus.StationMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Something done on a station block's screen (radio interface, APRS terminal, transfer interface): {@code action}
 * names it and {@code data} carries its values; the open {@link StationMenu} carries it out. Only a player with that
 * block's screen open can do it.
 */
public record SStationPacket(long pos, String action, CompoundTag data) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SStationPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_station"));

    public static final StreamCodec<ByteBuf, SStationPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, SStationPacket::pos,
            ByteBufCodecs.STRING_UTF8, SStationPacket::action,
            ByteBufCodecs.COMPOUND_TAG, SStationPacket::data,
            SStationPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if(context.player() instanceof ServerPlayer player && player.containerMenu instanceof StationMenu menu
                    && menu.getBlockPos().equals(BlockPos.of(pos)))
                menu.handleAction(player, action, data);
        });
    }

    public static void send(BlockPos pos, String action, CompoundTag data) {
        PacketDistributor.sendToServer(new SStationPacket(pos.asLong(), action, data));
    }

    public static void send(BlockPos pos, String action) {
        send(pos, action, new CompoundTag());
    }
}
