package com.arrl.radiocraft.common.radio.antenna.types.data;

import com.arrl.radiocraft.common.radio.antenna.AntennaData;
import net.minecraft.nbt.CompoundTag;

public class VerticalQuadLoopAntennaData extends AntennaData {

	private int sideLength;
	private boolean xAxis;

	public VerticalQuadLoopAntennaData(int length, boolean xAxis) {
		this.sideLength = length;
		this.xAxis = xAxis;
	}

	public int getSideLength() {
		return sideLength;
	}

	public boolean getXAxis() {
		return xAxis;
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag nbt = new CompoundTag();
		nbt.putInt("sideLength", sideLength);
		nbt.putBoolean("xAxis", xAxis);
		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		sideLength = nbt.getInt("sideLength");
		xAxis = nbt.getBoolean("xAxis");
	}

}