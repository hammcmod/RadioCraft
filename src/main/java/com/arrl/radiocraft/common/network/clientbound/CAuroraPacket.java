package com.arrl.radiocraft.common.network.clientbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.client.RadiocraftClientValues;
import com.arrl.radiocraft.common.radio.solar.Aurora;
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
 * Tells a client whether an aurora is lit in its dimension, how far from the equator it reaches and how much static
 * it adds there: the sky renderer draws it and HF radios under it get louder static. Sent when the aurora starts or
 * ends, on login and on dimension change.
 *
 * @param active   True while the aurora is lit.
 * @param latitude Distance from Z = 0 from which it is overhead.
 * @param noise    Extra static for HF radios under it.
 */
public record CAuroraPacket(boolean active, int latitude, float noise) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CAuroraPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("c_aurora"));

    public static final StreamCodec<ByteBuf, CAuroraPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, CAuroraPacket::active,
            ByteBufCodecs.VAR_INT, CAuroraPacket::latitude,
            ByteBufCodecs.FLOAT, CAuroraPacket::noise,
            CAuroraPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final CAuroraPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            RadiocraftClientValues.AURORA_ACTIVE = packet.active();
            RadiocraftClientValues.AURORA_LATITUDE = packet.latitude();
            RadiocraftClientValues.AURORA_NOISE = packet.noise();
        });
    }

    private static CAuroraPacket of(ServerLevel level) {
        return new CAuroraPacket(Aurora.isActive(level), RadiocraftServerConfig.AURORA_LATITUDE.get(), RadiocraftServerConfig.AURORA_STATIC.get().floatValue());
    }

    public static void sendToLevel(ServerLevel level) {
        PacketDistributor.sendToPlayersInDimension(level, of(level));
    }

    public static void sendToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, of(player.serverLevel()));
    }
}
