package com.arrl.radiocraft.common.radio.antenna.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.UnknownNullability;

public class QuarterWaveVerticalAntennaData extends AntennaData {

	private int height;

	public QuarterWaveVerticalAntennaData(int height) {
		this.height = height;
	}

	public int getHeight() {
		return height;
	}

	@Override
	public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
		CompoundTag nbt = new CompoundTag();
		nbt.putInt("height", height);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
		this.height = nbt.getInt("height");
	}
}
