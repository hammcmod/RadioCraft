package com.arrl.radiocraft.common.radio.morse;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * {@link CWBuffer#BUFFER_LENGTH} ticks of key state (true = key down), numbered so the receiving side can put buffers
 * back in order.
 */
public class CWBuffer {

	public static final int BUFFER_LENGTH = 20;

	/** Wire format: the id, then the key states packed into one int (bit i = tick i). */
	public static final StreamCodec<ByteBuf, CWBuffer> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			CWBuffer::getId,
			ByteBufCodecs.VAR_INT,
			CWBuffer::toMask,
			CWBuffer::fromMask
	);

	private final int id;
	private final boolean[] inputs;
	private int read = 0;

	public CWBuffer(int id, boolean[] inputs) {
		this.id = id;

		if(inputs.length == BUFFER_LENGTH)
			this.inputs = inputs;
		else {
			this.inputs = new boolean[BUFFER_LENGTH];

			int smallestLength = Math.min(BUFFER_LENGTH, inputs.length);
			System.arraycopy(inputs, 0, this.inputs, 0, smallestLength);
		}

	}

	/** Builds a buffer from the packed form written by {@link #toMask()}. */
	public static CWBuffer fromMask(int id, int mask) {
		boolean[] inputs = new boolean[BUFFER_LENGTH];
		for(int i = 0; i < BUFFER_LENGTH; i++)
			inputs[i] = (mask & (1 << i)) != 0;
		return new CWBuffer(id, inputs);
	}

	/** Packs the key states into an int, bit i set when tick i is key-down. */
	public int toMask() {
		int mask = 0;
		for(int i = 0; i < BUFFER_LENGTH; i++)
			if(inputs[i])
				mask |= 1 << i;
		return mask;
	}

	public int getId() {
		return id;
	}

	public boolean[] getInputs() {
		return inputs;
	}

	/** @return True if any tick in this buffer is key-down. */
	public boolean hasKeyDown() {
		return toMask() != 0;
	}

	/**
	 * @return The next boolean value from the inputs stored in this {@link CWBuffer}
	 */
	public boolean readNext() {
		return inputs[read++];
	}

	/**
	 * @return True if the current read index is at the end of the buffer, false otherwise.
	 */
	public boolean isFinished() {
		return read >= inputs.length;
	}

}
