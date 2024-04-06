package com.arrl.radiocraft.api.benetworks.power;

import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.capabilities.BasicEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Base class representing a {@link BENetworkObject} which consumes power. Each BE should have its own implementation
 * of this.
 */
public abstract class PowerNetworkObject extends BENetworkObject {

    protected BasicEnergyStorage energyStorage;
    protected LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> energyStorage);

    public PowerNetworkObject(int capacity, int maxReceive, int maxExtract) {
        this.energyStorage = new BasicEnergyStorage(capacity, maxReceive, maxExtract);
    }

    public BasicEnergyStorage getStorage() {
        return energyStorage;
    }

    public LazyOptional<IEnergyStorage> getStorageOptional() {
        return energyHandler;
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

    /**
     * Tick function to handle energy generation within this {@link PowerNetworkObject}.
     *
     * @param level The {@link Level} this {@link PowerNetworkObject} is in.
     * @param pos The {@link BlockPos} this {@link PowerNetworkObject} is on.
     */
    public void generate(Level level, BlockPos pos) {};

    /**
     * Tick function to handle energy consumption for this {@link PowerNetworkObject}
     *
     * @param level The {@link Level} this {@link PowerNetworkObject} is in.
     * @param pos The {@link BlockPos} this {@link PowerNetworkObject} is on.
     */
    public void consume(Level level, BlockPos pos) {};

    @Override
    public void save(CompoundTag nbt) {
        super.save(nbt);
        if(energyStorage != null)
            energyStorage.saveAdditional(nbt);
    }

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
