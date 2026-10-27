package com.arrl.radiocraft.common.network.serverbound;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import com.arrl.radiocraft.common.radio.voice.handheld.ManpackRadio;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Morse keyed on the HF manpack's screen, for the server to send out of the player's manpack. */
public record SManpackCWPacket(List<CWBuffer> buffers) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SManpackCWPacket> TYPE = new CustomPacketPayload.Type<>(Radiocraft.id("s_manpack_cw"));

	public static final StreamCodec<ByteBuf, SManpackCWPacket> STREAM_CODEC = StreamCodec.composite(
			CWBuffer.STREAM_CODEC.apply(ByteBufCodecs.list(SCWBufferPacket.MAX_BUFFERS)), SManpackCWPacket::buffers,
			SManpackCWPacket::new
	);

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public void handle(IPayloadContext context) {
		context.enqueueWork(() -> ManpackRadio.get(context.player().getUUID()).ifPresent(radio -> radio.transmitMorse(buffers)));
	}

	public static void send(List<CWBuffer> buffers) {
		PacketDistributor.sendToServer(new SManpackCWPacket(buffers));
	}
}
