package com.arrl.radiocraft.common.network.clientbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Morse the player's HF manpack received, to play back on their client at {@code strength}. */
public record CManpackCWPacket(List<CWBuffer> buffers, float strength) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<CManpackCWPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("c_manpack_cw"));

	public static final StreamCodec<ByteBuf, CManpackCWPacket> STREAM_CODEC = StreamCodec.composite(
			CWBuffer.STREAM_CODEC.apply(ByteBufCodecs.list(64)), CManpackCWPacket::buffers,
			ByteBufCodecs.FLOAT, CManpackCWPacket::strength,
			CManpackCWPacket::new
	);

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(final CManpackCWPacket packet, final IPayloadContext context) {
		context.enqueueWork(() -> com.arrl.radiocraft.client.ManpackClient.receiveCw(packet.buffers(), packet.strength()));
	}

	public static void send(ServerPlayer player, List<CWBuffer> buffers, float strength) {
		PacketDistributor.sendToPlayer(player, new CManpackCWPacket(buffers, strength));
	}
}
