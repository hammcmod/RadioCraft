package com.arrl.radiocraft.common.radio.morse;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;

public class CWSendBuffer {

	private final ResourceKey<Level> dimension;
	private final BlockPos pos;

	private final boolean[] partialBuffer = new boolean[CWBuffer.BUFFER_LENGTH];
	private int currentId = 0;
	private int currentIndex = 0;
	private int ticksSinceInput = 0;

	private boolean accumulateInput = false;

	public CWSendBuffer(ResourceKey<Level> dimension, BlockPos pos) {
		this.dimension = dimension;
		this.pos = pos;
	}

	public void tick() {
		if(ticksSinceInput < CWBuffer.BUFFER_LENGTH) {
			partialBuffer[currentIndex++] = accumulateInput;

			if(currentIndex == partialBuffer.length) {
				currentIndex = 0;
				//RadiocraftPackets.sendToServer(new CWBufferPacket(dimension, pos, List.of(new CWBuffer(currentId++, partialBuffer)), 1.0F));
			}
		}
		else {
			if(currentIndex != 0) {
				/*RadiocraftPackets.sendToServer(new CWBufferPacket(dimension, pos, Arrays.asList(
						new CWBuffer(currentId++, partialBuffer),
						new CWBuffer(currentId++, new boolean[CWBuffer.BUFFER_LENGTH])
				), 1.0F));*/
				currentIndex = 0;
			}
		}
		ticksSinceInput++;
		accumulateInput = false;
	}

	public void setShouldAccumulate() {
		accumulateInput = true;
		ticksSinceInput = 0;
	}

}
