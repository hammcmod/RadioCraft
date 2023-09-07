package com.arrl.radiocraft.common.radio.morse;

import com.arrl.radiocraft.common.init.RadiocraftPackets;
import com.arrl.radiocraft.common.network.packets.CWBufferPacket;
import net.minecraft.core.BlockPos;

import java.util.Arrays;
import java.util.List;

public class CWSendBuffer {

	private final BlockPos pos;

	private final boolean[] partialBuffer = new boolean[CWInputBuffer.BUFFER_LENGTH];
	private int currentId = 0;
	private int currentIndex = 0;
	private int ticksSinceInput = 0;

	private boolean accumulateInput = false;

	public CWSendBuffer(BlockPos pos) {
		this.pos = pos;
	}

	public void tick() {
		if(ticksSinceInput < CWInputBuffer.BUFFER_LENGTH) {
			partialBuffer[currentIndex++] = accumulateInput;

			if(currentIndex == partialBuffer.length) {
				currentIndex = 0;
				RadiocraftPackets.sendToServer(new CWBufferPacket(pos, List.of(new CWInputBuffer(currentId++, partialBuffer))));
			}
		}
		else {
			if(currentIndex != 0) {
				RadiocraftPackets.sendToServer(new CWBufferPacket(pos, Arrays.asList(new CWInputBuffer(currentId++, partialBuffer), new CWInputBuffer(currentId++, new boolean[CWInputBuffer.BUFFER_LENGTH]))));
				currentIndex = 0;
			}
			// Send an empty buffer to signify end of transmission
		}
		ticksSinceInput++;
		accumulateInput = false;
	}

	public void setShouldAccumulate() {
		accumulateInput = true;
		ticksSinceInput = 0;
	}

}
