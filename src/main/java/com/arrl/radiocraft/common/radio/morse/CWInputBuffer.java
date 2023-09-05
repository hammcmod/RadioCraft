package com.arrl.radiocraft.common.radio.morse;

public class CWInputBuffer {

	private final int id;
	private final boolean[] inputs;

	public CWInputBuffer(int id, boolean[] inputs) {
		this.id = id;

		if(inputs.length == 20)
			this.inputs = inputs;
		else {
			this.inputs = new boolean[20];

			int smallestLength = Math.min(20, inputs.length);
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
