package com.arrl.radiocraft.common.radio.antenna.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.UnknownNullability;

/** A delta loop's shape: the length of its three sides and how high its lowest corner is above the ground. */
public class DeltaLoopAntennaData extends AntennaData {

	private double perimeter;
	private double longestSide;
	private double shortestSide;
	private int height;

	public DeltaLoopAntennaData(double perimeter, double longestSide, double shortestSide, int height) {
		this.perimeter = perimeter;
		this.longestSide = longestSide;
		this.shortestSide = shortestSide;
		this.height = height;
	}

	public double getPerimeter() {
		return perimeter;
	}

	public double getLongestSide() {
		return longestSide;
	}

	public double getShortestSide() {
		return shortestSide;
	}

	/** Blocks of clear air under the loop's lowest corner. */
	public int getHeight() {
		return height;
	}

	@Override
	public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
		CompoundTag nbt = new CompoundTag();
		nbt.putDouble("perimeter", perimeter);
		nbt.putDouble("longestSide", longestSide);
		nbt.putDouble("shortestSide", shortestSide);
		nbt.putInt("height", height);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
		perimeter = nbt.getDouble("perimeter");
		longestSide = nbt.getDouble("longestSide");
		shortestSide = nbt.getDouble("shortestSide");
		height = nbt.getInt("height");
	}
}
