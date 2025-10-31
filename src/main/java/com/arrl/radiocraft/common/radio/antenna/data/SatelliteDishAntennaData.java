package com.arrl.radiocraft.common.radio.antenna.data;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Antenna data for Satellite Dish - stores facing direction for directional signal processing.
 */
public class SatelliteDishAntennaData extends AntennaData {

    private Direction facing;

    public SatelliteDishAntennaData(Direction facing) {
        this.facing = facing;
    }

    public Direction getFacing() {
        return facing;
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("facing", facing.ordinal());
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        facing = Direction.values()[nbt.getInt("facing")];
    }
}
