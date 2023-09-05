package com.arrl.radiocraft.common.network.packets;

import com.arrl.radiocraft.common.network.RadiocraftPacket;
import com.arrl.radiocraft.common.radio.morse.CWInputBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet for sending CW buffers both S->C and C->S
 */
public class CWBufferPacket implements RadiocraftPacket {

	private final BlockPos radio;
	private final Collection<CWInputBuffer> buffers;

	public CWBufferPacket(BlockPos radio, Collection<CWInputBuffer> buffers) {
		this.radio = radio;
		this.buffers = buffers;
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeLong(radio.asLong());
		buffer.writeInt(buffers.size());

		for(CWInputBuffer inputBuffer : buffers) {
			buffer.writeInt(inputBuffer.getId());
			for(boolean b : inputBuffer.getInputs())
				buffer.writeBoolean(b);
		}
	}

	public static CWBufferPacket decode(FriendlyByteBuf buffer) {
		BlockPos radioPos = BlockPos.of(buffer.readLong());
		int bufferCount = buffer.readInt();

		List<CWInputBuffer> buffers = new ArrayList<>();
		for(int z = 0; z < bufferCount; z++) {
			int id = buffer.readInt();

			boolean[] values = new boolean[20];
			for(int i = 0; i < 20; i++) {
				values[i] = buffer.readBoolean();
			}

			buffers.add(new CWInputBuffer(id, values));
		}

		return new CWBufferPacket(radioPos, buffers);
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
