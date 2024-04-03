package com.arrl.radiocraft.common.network.packets.clientbound;

import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.network.RadiocraftPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.function.Supplier;

/**
 * Packet for updating the solar event state on the client, for use in static volume.
 */
public class CAntennaWirePacket implements RadiocraftPacket {

	private final int id;
	private final BlockPos endPos;

	public CAntennaWirePacket(int id, BlockPos endPos) {
		this.id = id;
		this.endPos = endPos;
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeInt(id);
		buffer.writeLong(endPos.asLong());
	}

	public static CAntennaWirePacket decode(FriendlyByteBuf buffer) {
		return new CAntennaWirePacket(buffer.readInt(), BlockPos.of(buffer.readLong()));
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(() -> {
			Entity entity = Minecraft.getInstance().level.getEntity(id);
			if(entity instanceof AntennaWire wire)
				wire.setEndPos(endPos);
		});
		context.get().setPacketHandled(true);
	}

}