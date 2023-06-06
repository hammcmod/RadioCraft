package com.arrl.radiocraft.common.radio.antenna.types.data;

import com.arrl.radiocraft.common.radio.antenna.AntennaData;
import net.minecraft.nbt.CompoundTag;

public class HorizontalQuadLoopAntennaData extends AntennaData {

	private int sideLength;

	public HorizontalQuadLoopAntennaData(int length) {
		this.sideLength = length;
	}

	public int getSideLength() {
		return sideLength;
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag nbt = new CompoundTag();
		nbt.putInt("sideLength", sideLength);
		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		sideLength = nbt.getInt("sideLength");
	}

}