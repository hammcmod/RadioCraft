package com.arrl.radiocraft.common.capabilities;

import net.minecraftforge.energy.EnergyStorage;

public class BasicEnergyStorage extends EnergyStorage {

	public BasicEnergyStorage(int capacity) {
		this(capacity, capacity, capacity, 0);
	}

	public BasicEnergyStorage(int capacity, int maxTransfer) {
		this(capacity, maxTransfer, maxTransfer, 0);
	}

	public BasicEnergyStorage(int capacity, int maxReceive, int maxExtract) {
		this(capacity, maxReceive, maxExtract, 0);
	}

	public BasicEnergyStorage(int capacity, int maxReceive, int maxExtract, int energy) {
		super(capacity, maxReceive, maxExtract, energy);
	}

	public void setEnergy(int energy) {
		this.energy = Math.max(0 , Math.min(capacity, energy));
	}

}
