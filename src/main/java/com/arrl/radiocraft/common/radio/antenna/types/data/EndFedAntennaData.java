package com.arrl.radiocraft.common.radio.antenna.types.data;

import com.arrl.radiocraft.common.radio.antenna.AntennaData;
import net.minecraft.nbt.CompoundTag;

public class EndFedAntennaData extends AntennaData {

	private double length;

	public EndFedAntennaData(double length) {
		this.length = length;
	}

	public double getLength() {
		return length;
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag nbt = new CompoundTag();
		nbt.putDouble("length", length);
		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		length = nbt.getDouble("length");
	}

}