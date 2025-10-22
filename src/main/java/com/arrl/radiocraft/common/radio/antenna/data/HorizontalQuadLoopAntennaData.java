package com.arrl.radiocraft.common.radio.antenna.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.UnknownNullability;

public class HorizontalQuadLoopAntennaData extends AntennaData {

	private int sideLength;

	public HorizontalQuadLoopAntennaData(int length) {
		this.sideLength = length;
	}

	public int getSideLength() {
		return sideLength;
	}

	@Override
	public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
		CompoundTag nbt = new CompoundTag();
		nbt.putInt("sideLength", sideLength);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
		sideLength = nbt.getInt("sideLength");
	}
}