package com.arrl.radiocraft.common.radio.morse;

import com.arrl.radiocraft.common.network.serverbound.SCWBufferPacket;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.List;

/**
 * Client side: records the morse key state every tick while the radio's screen is open and ships it to the server in
 * {@link CWBuffer}s ({@link SCWBufferPacket}). A buffer is sent as soon as it is full; once the key has been up for a
 * whole buffer length the partial buffer is flushed, followed by an empty one so playback ends cleanly.
 */
public class CWSendBuffer {

	private final BlockPos pos;

	private final boolean[] partialBuffer = new boolean[CWBuffer.BUFFER_LENGTH];
	private int currentId = 0;
	private int currentIndex = 0;
	private int ticksSinceInput = 0;

	private boolean accumulateInput = false;

	public CWSendBuffer(BlockPos pos) {
		this.pos = pos;
	}

	public void tick() {
		if(ticksSinceInput < CWBuffer.BUFFER_LENGTH) {
			partialBuffer[currentIndex++] = accumulateInput;

			if(currentIndex == partialBuffer.length) {
				currentIndex = 0;
				PacketDistributor.sendToServer(new SCWBufferPacket(pos, List.of(new CWBuffer(currentId++, partialBuffer.clone()))));
			}
		}
		else {
			if(currentIndex != 0) {
				PacketDistributor.sendToServer(new SCWBufferPacket(pos, List.of(
						new CWBuffer(currentId++, partialBuffer.clone()),
						new CWBuffer(currentId++, new boolean[CWBuffer.BUFFER_LENGTH])
				)));
				Arrays.fill(partialBuffer, false);
				currentIndex = 0;
			}
		}
		ticksSinceInput++;
		accumulateInput = false;
	}

	/** Marks the key as down for the tick being recorded. */
	public void setShouldAccumulate() {
		accumulateInput = true;
		ticksSinceInput = 0;
	}

}
