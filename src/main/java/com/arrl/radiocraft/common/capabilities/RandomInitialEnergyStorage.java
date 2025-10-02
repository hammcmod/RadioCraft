package com.arrl.radiocraft.common.capabilities;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;

/**
 * Custom ComponentEnergyStorage that initializes with random energy between 50-70% of capacity
 * ONLY when the item is first created (DataComponent is null/absent).
 */
public class RandomInitialEnergyStorage extends ComponentEnergyStorage {

    private final MutableDataComponentHolder parent;
    private final DataComponentType<Integer> energyComponent;

    public RandomInitialEnergyStorage(MutableDataComponentHolder parent, DataComponentType<Integer> energyComponent, int capacity, int maxReceive, int maxExtract) {
        super(parent, energyComponent, capacity, maxReceive, maxExtract);
        this.parent = parent;
        this.energyComponent = energyComponent;
        initializeRandomEnergyIfNew();
    }

    public RandomInitialEnergyStorage(MutableDataComponentHolder parent, DataComponentType<Integer> energyComponent, int capacity, int maxTransfer) {
        super(parent, energyComponent, capacity, maxTransfer);
        this.parent = parent;
        this.energyComponent = energyComponent;
        initializeRandomEnergyIfNew();
    }

    public RandomInitialEnergyStorage(MutableDataComponentHolder parent, DataComponentType<Integer> energyComponent, int capacity) {
        super(parent, energyComponent, capacity);
        this.parent = parent;
        this.energyComponent = energyComponent;
        initializeRandomEnergyIfNew();
    }

    /**
     * Initializes random energy ONLY if the DataComponent has never been set (is absent/null).
     * This ensures batteries only get random initial charge when first crafted,
     * not when they are depleted to zero.
     */
    private void initializeRandomEnergyIfNew() {
        // Check if the component has ever been set (null means brand new item)
        if (!parent.has(energyComponent)) {
            // Generate random energy between 50% and 70% of capacity
            RandomSource random = RandomSource.create();
            float randomPercentage = 0.5f + (random.nextFloat() * 0.2f); // 50% to 70%
            int initialEnergy = Math.round(getMaxEnergyStored() * randomPercentage);
            setEnergy(initialEnergy);
        }
    }
}