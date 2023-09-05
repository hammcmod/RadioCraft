package com.arrl.radiocraft.common.network.packets;

import com.arrl.radiocraft.common.network.RadiocraftPacket;
import com.arrl.radiocraft.common.radio.morse.CWInputBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.function.Supplier;

/**
 * Packet for sending CW buffers both S->C and C->S
 */
public class CWBufferPacket implements RadiocraftPacket {

	private final BlockPos radio;
	private final CWInputBuffer buffer;

	public CWBufferPacket(BlockPos radio, CWInputBuffer buffer) {
		this.radio = radio;
		this.buffer = buffer;
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeLong(radio.asLong());
		buffer.writeInt(this.buffer.getId());
		for(boolean b : this.buffer.getInputs())
			buffer.writeBoolean(b);
	}

	public static CWBufferPacket decode(FriendlyByteBuf buffer) {
		BlockPos radioPos = BlockPos.of(buffer.readLong());
		int id = buffer.readInt();

		boolean[] values = new boolean[20];
		for(int i = 0; i < 20; i++)
			values[i] = buffer.readBoolean();

		return new CWBufferPacket(radioPos, new CWInputBuffer(id, values));
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(() -> {
			if(context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {

			}
			else {

			}
		});
	}

}
