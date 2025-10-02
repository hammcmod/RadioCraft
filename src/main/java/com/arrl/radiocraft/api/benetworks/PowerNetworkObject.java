package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.capabilities.BasicEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Base class representing a {@link BENetworkObject} which consumes power. Each BE should have its own implementation
 * of this.
 */
public abstract class PowerNetworkObject extends BENetworkObject {

    /**
     * The {@link IEnergyStorage} for this {@link PowerNetworkObject}.
     */
    protected BasicEnergyStorage energyStorage;

    /**
     * Creates a new {@link PowerNetworkObject}.
     * @param level The {@link Level} this object is in.
     * @param pos The {@link BlockPos} of this object.
     * @param capacity The maximum amount of energy this object can hold.
     * @param maxReceive The maximum amount of energy this object can receive.
     * @param maxExtract The maximum amount of energy this object can extract.
     */
    public PowerNetworkObject(Level level, BlockPos pos, int capacity, int maxReceive, int maxExtract) {
        super(level, pos);
        this.energyStorage = new BasicEnergyStorage(capacity, maxReceive, maxExtract);
    }

    /**
     * Gets the {@link IEnergyStorage} for this {@link PowerNetworkObject}.
     * @return The {@link IEnergyStorage} for this {@link PowerNetworkObject}.
     */
    public BasicEnergyStorage getStorage() {
        return energyStorage;
    }

    /**
     * Is this {@link PowerNetworkObject} an indirect consumer?
     * @return True if this {@link PowerNetworkObject} can receive energy from a non-direct provider (battery, charge
     * controller)
     */
    public abstract boolean isIndirectConsumer();

    /**
     * Is this {@link PowerNetworkObject} a direct consumer?
     * @return True if this {@link PowerNetworkObject} can receive energy from a direct provider (solar panel)
     */
    public abstract boolean isDirectConsumer();

    /**
     * Tries to consume energy.
     * @param amount The amount of energy to consume.
     * @param simulate Whether to simulate the consumption.
     * @return True if the energy was consumed.
     */
    protected boolean tryConsumeEnergy(int amount, boolean simulate) {
        return energyStorage.extractEnergy(amount, simulate) == amount;
    }

    /**
     * Saves the NBT.
     * @param nbt The {@link CompoundTag} to save to.
     */
    @Override
    public void save(CompoundTag nbt) {
        super.save(nbt);
        if(energyStorage != null)
            energyStorage.saveAdditional(nbt);
    }

    /**
     * Loads the NBT
     * @param cap The {@link IBENetworks} capability to use.
     * @param nbt The {@link CompoundTag} to load from.
     */
    @Override
    public void load(IBENetworks cap, CompoundTag nbt) {
        super.load(cap, nbt);
        energyStorage = new BasicEnergyStorage(
                nbt.getInt("capacity"),
                nbt.getInt("maxReceive"),
                nbt.getInt("maxExtract"),
                nbt.getInt("energy")
        );
    }
}
