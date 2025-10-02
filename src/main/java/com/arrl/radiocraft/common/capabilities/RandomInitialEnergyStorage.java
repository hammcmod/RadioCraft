package com.arrl.radiocraft.common.capabilities;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;

/**
 * Custom ComponentEnergyStorage that initializes with random energy between 50-70% of capacity
 */
public class RandomInitialEnergyStorage extends ComponentEnergyStorage {

    private boolean initialized = false;

    public RandomInitialEnergyStorage(MutableDataComponentHolder parent, DataComponentType<Integer> energyComponent, int capacity, int maxReceive, int maxExtract) {
        super(parent, energyComponent, capacity, maxReceive, maxExtract);
        initializeRandomEnergy();
    }

    public RandomInitialEnergyStorage(MutableDataComponentHolder parent, DataComponentType<Integer> energyComponent, int capacity, int maxTransfer) {
        super(parent, energyComponent, capacity, maxTransfer);
        initializeRandomEnergy();
    }

    public RandomInitialEnergyStorage(MutableDataComponentHolder parent, DataComponentType<Integer> energyComponent, int capacity) {
        super(parent, energyComponent, capacity);
        initializeRandomEnergy();
    }

    private void initializeRandomEnergy() {
        if (!initialized && super.getEnergyStored() == 0) {
            // Generate random energy between 50% and 70% of capacity
            RandomSource random = RandomSource.create();
            float randomPercentage = 0.5f + (random.nextFloat() * 0.2f); // 50% to 70%
            int initialEnergy = Math.round(getMaxEnergyStored() * randomPercentage);
            setEnergy(initialEnergy);
            initialized = true;
        }
    }

    @Override
    public int getEnergyStored() {
        if (!initialized) {
            initializeRandomEnergy();
        }
        return super.getEnergyStored();
    }
}