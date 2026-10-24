package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.CallsignPrompt;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** A player choosing their own callsign from the prompt. The server checks it (see {@code Callsigns#claim}). */
public record SClaimCallsignPacket(String callsign) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SClaimCallsignPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_claim_callsign"));

    public static final StreamCodec<ByteBuf, SClaimCallsignPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(16), SClaimCallsignPacket::callsign,
            SClaimCallsignPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if(context.player() instanceof ServerPlayer player)
                CallsignPrompt.claimFromPrompt(player, callsign);
        });
    }

    public static void send(String callsign) {
        PacketDistributor.sendToServer(new SClaimCallsignPacket(callsign));
    }
}
