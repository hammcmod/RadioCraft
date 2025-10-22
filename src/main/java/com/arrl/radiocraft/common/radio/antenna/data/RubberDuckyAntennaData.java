package com.arrl.radiocraft.common.radio.antenna.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.UnknownNullability;

public class RubberDuckyAntennaData extends AntennaData {

    public RubberDuckyAntennaData(double length) {
        this.length = length;
    }

    public double getLength() {
        return length;
    }

    private static final String TAG_LENGTH = "length";

    private double length;


    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putDouble(TAG_LENGTH, length);
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        length = nbt.getDouble(TAG_LENGTH);
    }
}
