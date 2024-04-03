package com.arrl.radiocraft.common.network.packets;

import com.arrl.radiocraft.common.blockentities.HFRadioBlockEntity;
import com.arrl.radiocraft.common.network.RadiocraftPacket;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import com.arrl.radiocraft.common.radio.morse.CWReceiveBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
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

	private final ResourceKey<Level> dimension;
	private final BlockPos pos;
	private final Collection<CWBuffer> buffers;
	private final float strength; // Strength is only used for S->C communication.

	public CWBufferPacket(ResourceKey<Level> dimension, BlockPos pos, Collection<CWBuffer> buffers, float strength) {
		this.dimension = dimension;
		this.pos = pos;
		this.buffers = buffers;
		this.strength = strength;
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeResourceKey(dimension);
		buffer.writeLong(pos.asLong());
		buffer.writeFloat(strength);
		buffer.writeInt(buffers.size());

		for(CWBuffer inputBuffer : buffers) {
			buffer.writeInt(inputBuffer.getId());
			for(boolean b : inputBuffer.getInputs())
				buffer.writeBoolean(b);
		}
	}

	public static CWBufferPacket decode(FriendlyByteBuf buffer) {
		ResourceKey<Level> level = buffer.readResourceKey(Registries.DIMENSION);
		BlockPos radioPos = BlockPos.of(buffer.readLong());
		float strength = buffer.readFloat();
		int bufferCount = buffer.readInt();

		List<CWBuffer> buffers = new ArrayList<>();
		for(int z = 0; z < bufferCount; z++) {
			int id = buffer.readInt();

			boolean[] values = new boolean[CWBuffer.BUFFER_LENGTH];
			for(int i = 0; i < CWBuffer.BUFFER_LENGTH; i++) {
				values[i] = buffer.readBoolean();
			}

			buffers.add(new CWBuffer(id, values));
		}

		return new CWBufferPacket(level, radioPos, buffers, strength);
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(() -> {
			if(context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
				if(Minecraft.getInstance().level.getBlockEntity(pos) instanceof HFRadioBlockEntity be) {
					// Client receiving these packets will just forward them all to the BE and let them get re-ordered in there.
					CWReceiveBuffer radioBuffer = be.getCWReceiveBuffer();
					for(CWBuffer buffer : buffers) {
						radioBuffer.addToBuffer(buffer, strength);
					}
				}
			}
			else {
				Level level = context.get().getSender().getServer().getLevel(dimension);
				if(level != null) {
					if(level.getBlockEntity(pos) instanceof HFRadioBlockEntity radio && radio.getCWEnabled()) {
						// When packet is received by server it calculates all the receiving strengths etc. and sends to other clients who need the info.
						radio.transmitMorse(buffers);
					}
				}
			}
		});
		context.get().setPacketHandled(true);
	}

}
