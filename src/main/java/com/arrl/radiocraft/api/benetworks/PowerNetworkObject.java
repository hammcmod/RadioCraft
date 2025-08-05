package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.capabilities.BasicEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * Base class representing a {@link BENetworkObject} which consumes power. Each BE should have its own implementation
 * of this.
 */
public abstract class PowerNetworkObject extends BENetworkObject {

    protected BasicEnergyStorage energyStorage;

    public PowerNetworkObject(Level level, BlockPos pos, int capacity, int maxReceive, int maxExtract) {
        super(level, pos);
        this.energyStorage = new BasicEnergyStorage(capacity, maxReceive, maxExtract);
    }

    public BasicEnergyStorage getStorage() {
        return energyStorage;
    }

    /**
     * @return True if this {@link PowerNetworkObject} can receive energy from a non-direct provider (battery, charge
     * controller)
     */
    public abstract boolean isIndirectConsumer();

    /**
     * @return True if this {@link PowerNetworkObject} can receive energy from a direct provider (solar panel)
     */
    public abstract boolean isDirectConsumer();

    protected boolean tryConsumeEnergy(int amount, boolean simulate) {
        return energyStorage.extractEnergy(amount, simulate) == amount;
    }

    @Override
    public void save(CompoundTag nbt) {
        super.save(nbt);
        if(energyStorage != null)
            energyStorage.saveAdditional(nbt);
    }

    @Override
    public void load(IBENetworks cap, CompoundTag nbt) {
        super.load(cap, nbt);
        if (nbt.contains("capacity") && nbt.contains("maxReceive") && nbt.contains("maxExtract")) {
            energyStorage = new BasicEnergyStorage(
                    nbt.getInt("capacity"),
                    nbt.getInt("maxReceive"),
                    nbt.getInt("maxExtract"),
                    nbt.getInt("energy")
            );
        }
    }
}