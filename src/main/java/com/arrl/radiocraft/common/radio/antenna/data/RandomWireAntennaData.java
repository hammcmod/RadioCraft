package com.arrl.radiocraft.common.radio.antenna.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.UnknownNullability;

/** A random wire: one run of antenna wire of any shape, measured along the wire from the 9:1 balun to its far end. */
public class RandomWireAntennaData extends AntennaData {

	private double length;
	private int segments;

	public RandomWireAntennaData(double length, int segments) {
		this.length = length;
		this.segments = segments;
	}

	public double getLength() {
		return length;
	}

	/** How many wires make up the run (it may bend at poles and connectors). */
	public int getSegments() {
		return segments;
	}

	@Override
	public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
		CompoundTag nbt = new CompoundTag();
		nbt.putDouble("length", length);
		nbt.putInt("segments", segments);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
		length = nbt.getDouble("length");
		segments = nbt.getInt("segments");
	}
}
