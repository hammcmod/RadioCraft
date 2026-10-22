package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.DigitalInterfaceBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.menus.DigitalInterfaceMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Something done on a digital interface's screen: {@code action} names it ("beacon", "message", "claim", "callsign",
 * "settings", "read", "rtty" or "file") and {@code data} carries its values. Only a player with that screen open can do it.
 */
public record SDigitalInterfacePacket(long pos, String action, CompoundTag data) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SDigitalInterfacePacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_digital_interface"));

    public static final StreamCodec<ByteBuf, SDigitalInterfacePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, SDigitalInterfacePacket::pos,
            ByteBufCodecs.STRING_UTF8, SDigitalInterfacePacket::action,
            ByteBufCodecs.COMPOUND_TAG, SDigitalInterfacePacket::data,
            SDigitalInterfacePacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if(!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof DigitalInterfaceMenu menu)
                    || !menu.blockEntity.getBlockPos().equals(BlockPos.of(pos)))
                return;
            DigitalInterfaceBlockEntity tnc = menu.blockEntity;
            switch(action) {
                case "beacon" -> {
                    if(!tnc.beacon())
                        player.displayClientMessage(Component.translatable(failureKey(tnc)), true);
                }
                case "message" -> {
                    if(!tnc.sendMessage(data.getString("to"), data.getString("text")))
                        player.displayClientMessage(Component.translatable(failureKey(tnc)), true);
                }
                case "claim" -> {
                    if(!tnc.claim(player))
                        player.displayClientMessage(Component.translatable("screen.radiocraft.digital_interface.no_callsign"), true);
                }
                case "callsign" -> tnc.setCallsign(data.getString("callsign"), player.getGameProfile().getName());
                case "settings" -> tnc.applySettings(data);
                case "read" -> tnc.markRead();
                case "file" -> {
                    if(!tnc.sendFile(data.getString("to")))
                        player.displayClientMessage(Component.translatable(tnc.isTransferring() ? "screen.radiocraft.digital_interface.busy"
                                : tnc.getOutbox().getStackInSlot(0).isEmpty() ? "screen.radiocraft.digital_interface.nothing_to_send" : failureKey(tnc)), true);
                }
                case "rtty" -> {
                    if(!tnc.typeRtty(data.getString("text")))
                        player.displayClientMessage(Component.translatable(failureKey(tnc)), true);
                }
                default -> {}
            }
        });
    }

    private static String failureKey(DigitalInterfaceBlockEntity tnc) {
        if(tnc.getCallsign().isEmpty())
            return "screen.radiocraft.digital_interface.set_callsign_first";
        return tnc.getRadios().stream().anyMatch(RadioBlockEntity::isPowered)
                ? "screen.radiocraft.digital_interface.send_failed" : "screen.radiocraft.digital_interface.no_radio";
    }

    public static void send(BlockPos pos, String action, CompoundTag data) {
        PacketDistributor.sendToServer(new SDigitalInterfacePacket(pos.asLong(), action, data));
    }

    public static void send(BlockPos pos, String action) {
        send(pos, action, new CompoundTag());
    }
}
