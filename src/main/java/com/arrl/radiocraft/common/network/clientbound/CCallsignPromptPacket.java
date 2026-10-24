package com.arrl.radiocraft.common.network.clientbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.CallsignScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * The callsign prompt (see {@link com.arrl.radiocraft.common.radio.CallsignPrompt}): opens it, tells it why a claim
 * failed, or closes it once the claim went through.
 *
 * @param state {@link #OPEN} (text: the digits allowed), {@link #ERROR} (text: a translation key) or {@link #DONE}.
 */
public record CCallsignPromptPacket(int state, String text) implements CustomPacketPayload {

    public static final int OPEN = 0, ERROR = 1, DONE = 2;

    public static final CustomPacketPayload.Type<CCallsignPromptPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("c_callsign_prompt"));

    public static final StreamCodec<ByteBuf, CCallsignPromptPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CCallsignPromptPacket::state,
            ByteBufCodecs.STRING_UTF8, CCallsignPromptPacket::text,
            CCallsignPromptPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final CCallsignPromptPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> CallsignScreen.handle(packet.state(), packet.text()));
    }

    public static void send(ServerPlayer player, int state, String text) {
        PacketDistributor.sendToPlayer(player, new CCallsignPromptPacket(state, text));
    }
}
