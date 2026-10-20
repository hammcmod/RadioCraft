package com.arrl.radiocraft.common.radio.antenna.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.UnknownNullability;

/**
 * An inverted V dipole: two arms sloping down from a high centre. Arm lengths are the wire lengths (so they include
 * the drop); the drops are how far below the centre each end sits.
 */
public class InvertedVAntennaData extends AntennaData {

	private double armLength1;
	private double armLength2;
	private int drop1;
	private int drop2;

	public InvertedVAntennaData(double armLength1, double armLength2, int drop1, int drop2) {
		this.armLength1 = armLength1;
		this.armLength2 = armLength2;
		this.drop1 = drop1;
		this.drop2 = drop2;
	}

	public double getArmLength1() {
		return armLength1;
	}

	public double getArmLength2() {
		return armLength2;
	}

	public int getDrop1() {
		return drop1;
	}

	public int getDrop2() {
		return drop2;
	}

	@Override
	public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
		CompoundTag nbt = new CompoundTag();
		nbt.putDouble("armLength1", armLength1);
		nbt.putDouble("armLength2", armLength2);
		nbt.putInt("drop1", drop1);
		nbt.putInt("drop2", drop2);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
		armLength1 = nbt.getDouble("armLength1");
		armLength2 = nbt.getDouble("armLength2");
		drop1 = nbt.getInt("drop1");
		drop2 = nbt.getInt("drop2");
	}
}
