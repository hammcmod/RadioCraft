package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.common.radio.Callsigns;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.VHFRepeaterBlockEntity;
import com.arrl.radiocraft.common.menus.VHFRepeaterMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Changes some of a repeater's settings from its screen. {@code settings} holds only the changed keys (see
 * {@link VHFRepeaterBlockEntity#applySettings}, which checks every value). Only a player with the repeater's screen
 * open can change it.
 */
public record SRepeaterSettingsPacket(long pos, CompoundTag settings) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SRepeaterSettingsPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_repeater_settings"));

    public static final StreamCodec<ByteBuf, SRepeaterSettingsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, SRepeaterSettingsPacket::pos,
            ByteBufCodecs.COMPOUND_TAG, SRepeaterSettingsPacket::settings,
            SRepeaterSettingsPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if(context.player().containerMenu instanceof VHFRepeaterMenu menu && menu.blockEntity.getBlockPos().equals(BlockPos.of(pos))) {
                if(settings.contains("callsign") && context.player() instanceof ServerPlayer player) { // Only what repeater_callsign_mode allows.
                    String callsign = Callsigns.resolveRepeaterCallsign(player, player.level(), menu.blockEntity.getBlockPos(), settings.getString("callsign"));
                    if(callsign == null) {
                        settings.remove("callsign");
                        player.displayClientMessage(Component.translatable("message.radiocraft.repeater_callsign." + Callsigns.repeaterMode().name().toLowerCase(java.util.Locale.ROOT),
                                Callsigns.zoneDigit(player.level(), menu.blockEntity.getBlockPos())), true);
                    }
                    else
                        settings.putString("callsign", callsign);
                }
                menu.blockEntity.applySettings(settings);
            }
        });
    }

    public static void send(BlockPos pos, CompoundTag settings) {
        PacketDistributor.sendToServer(new SRepeaterSettingsPacket(pos.asLong(), settings));
    }
}
