package com.arrl.radiocraft.common.network.packets;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.AbstractRadioBlockEntity;
import com.arrl.radiocraft.common.network.RadiocraftPacket;
import com.arrl.radiocraft.common.radio.morse.CWInputBuffer;
import com.arrl.radiocraft.common.radio.morse.CWRadioBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
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

	private final BlockPos pos;
	private final Collection<CWInputBuffer> buffers;

	public CWBufferPacket(BlockPos pos, Collection<CWInputBuffer> buffers) {
		this.pos = pos;
		this.buffers = buffers;
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeLong(pos.asLong());
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

			boolean[] values = new boolean[CWInputBuffer.BUFFER_LENGTH];
			for(int i = 0; i < CWInputBuffer.BUFFER_LENGTH; i++) {
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
				BlockEntity be = Minecraft.getInstance().level.getBlockEntity(pos); // Client receiving these packets will just forward them all to the BE and let them get re-ordered in there.
				if(be instanceof AbstractRadioBlockEntity radio) {
					CWRadioBuffer radioBuffer = radio.getCWBuffer();
					for(CWInputBuffer buffer : buffers) {
						radioBuffer.addToBuffer(buffer);
					}
				}
			}
			else {
				Radiocraft.LOGGER.info("CW Input Buffer Received" + buffers.size());
			}
		});
	}

}
