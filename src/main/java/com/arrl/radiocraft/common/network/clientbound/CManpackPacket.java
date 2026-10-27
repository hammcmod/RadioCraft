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

/** The player's HF manpack, for their client: the received signal (for the S-meter and the static) and whether it is transmitting. */
public record CManpackPacket(float signal, boolean transmitting) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<CManpackPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("c_manpack"));

	public static final StreamCodec<ByteBuf, CManpackPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, CManpackPacket::signal,
			ByteBufCodecs.BOOL, CManpackPacket::transmitting,
			CManpackPacket::new
	);

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(final CManpackPacket packet, final IPayloadContext context) {
		context.enqueueWork(() -> com.arrl.radiocraft.client.ManpackClient.update(packet.signal(), packet.transmitting()));
	}

	public static void send(ServerPlayer player, float signal, boolean transmitting) {
		PacketDistributor.sendToPlayer(player, new CManpackPacket(signal, transmitting));
	}
}
