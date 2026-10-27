package com.arrl.radiocraft.common.network.clientbound;

import com.arrl.radiocraft.Radiocraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * What the handheld direction finder in a player's hand hears, a few times a second: whether a station is on its
 * frequency, the compass bearing to it, and how strong (0 to 1, for the beep's volume).
 */
public record CHandheldDfPacket(boolean hearing, float bearing, float strength) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<CHandheldDfPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("c_handheld_df"));

	public static final StreamCodec<ByteBuf, CHandheldDfPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, CHandheldDfPacket::hearing,
			ByteBufCodecs.FLOAT, CHandheldDfPacket::bearing,
			ByteBufCodecs.FLOAT, CHandheldDfPacket::strength,
			CHandheldDfPacket::new
	);

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(final CHandheldDfPacket packet, final IPayloadContext context) {
		context.enqueueWork(() -> com.arrl.radiocraft.client.HandheldDfClient.update(packet.hearing(), packet.bearing(), packet.strength()));
	}

	public static void send(ServerPlayer player, boolean hearing, float bearing, float strength) {
		PacketDistributor.sendToPlayer(player, new CHandheldDfPacket(hearing, bearing, strength));
	}
}
