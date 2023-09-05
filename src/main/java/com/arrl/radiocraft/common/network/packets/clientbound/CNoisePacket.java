package com.arrl.radiocraft.common.network.packets.clientbound;

import com.arrl.radiocraft.client.RadiocraftClientValues;
import com.arrl.radiocraft.common.network.RadiocraftPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.function.Supplier;

/**
 * Packet for updating the solar event state on the client, for use in static volume.
 */
public class CNoisePacket implements RadiocraftPacket {

	private final float noise;

	public CNoisePacket(float noise) {
		this.noise = noise;
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeFloat(noise);
	}

	public static CNoisePacket decode(FriendlyByteBuf buffer) {
		return new CNoisePacket(buffer.readFloat());
	}

	@Override
	public void handle(Supplier<Context> context) {
		RadiocraftClientValues.NOISE_VOLUME = noise;
		context.get().setPacketHandled(true);
	}

}