package com.arrl.radiocraft.common.radio.morse;

public class CWInputBuffer {

	public static final int BUFFER_LENGTH = 20;
	private final int id;
	private final boolean[] inputs;

	public CWInputBuffer(int id, boolean[] inputs) {
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

}
