package com.arrl.radiocraft.common.network.clientbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.RadiocraftClientValues;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Tells a client the noise floor of the solar weather in its dimension, which scales the static volume of radios
 * ({@link com.arrl.radiocraft.common.sounds.RadioStaticSoundInstance}). Sent when a new solar event starts, when a
 * player logs in and when a player changes dimension.
 *
 * @param noise The solar event's noise, 0 to 1.
 */
public record CNoisePacket(float noise) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CNoisePacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("c_noise"));

    public static final StreamCodec<ByteBuf, CNoisePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            CNoisePacket::noise,
            CNoisePacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final CNoisePacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> RadiocraftClientValues.NOISE_VOLUME = packet.noise());
    }

    /** Sends the current noise for {@code level} to every player in it. */
    public static void sendToLevel(ServerLevel level) {
        SolarEventInstance event = SolarEventManager.getEvent(level);
        if(event != null)
            PacketDistributor.sendToPlayersInDimension(level, new CNoisePacket(event.getEvent().getNoise()));
    }

    /** Sends the current noise of the player's dimension to that player. */
    public static void sendToPlayer(ServerPlayer player) {
        SolarEventInstance event = SolarEventManager.getEvent(player.level());
        if(event != null)
            PacketDistributor.sendToPlayer(player, new CNoisePacket(event.getEvent().getNoise()));
    }
}
