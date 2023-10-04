package com.arrl.radiocraft.common.radio.morse;

public class CWBuffer {

	public static final int BUFFER_LENGTH = 20;
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

	public int getId() {
		return id;
	}

	public boolean[] getInputs() {
		return inputs;
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
