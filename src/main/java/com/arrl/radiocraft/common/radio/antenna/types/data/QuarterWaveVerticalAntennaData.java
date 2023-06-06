package com.arrl.radiocraft.common.radio.antenna.types.data;

import com.arrl.radiocraft.common.radio.antenna.AntennaData;
import net.minecraft.nbt.CompoundTag;

public class QuarterWaveVerticalAntennaData extends AntennaData {

	private int height;

	public QuarterWaveVerticalAntennaData(int height) {
		this.height = height;
	}

	public int getHeight() {
		return height;
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag nbt = new CompoundTag();
		nbt.putInt("height", height);
		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		this.height = nbt.getInt("height");
	}

}
