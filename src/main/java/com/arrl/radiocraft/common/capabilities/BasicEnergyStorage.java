package com.arrl.radiocraft.common.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.energy.EnergyStorage;

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

	public void setMaxEnergy(int value) {
		this.capacity = value;
	}

	public void setMaxReceive(int value) {
		this.maxReceive = value;
	}

	public void setMaxExtract(int value) {
		this.maxExtract = value;
	}

	public int getMaxReceive() {
		return this.maxReceive;
	}

	public int getMaxExtract() {
		return this.maxExtract;
	}

	public void saveAdditional(CompoundTag nbt) {
		nbt.putInt("capacity", capacity);
		nbt.putInt("maxReceive", maxReceive);
		nbt.putInt("maxExtract", maxExtract);
		nbt.putInt("energy", energy);
	}

}
