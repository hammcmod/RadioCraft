package com.arrl.radiocraft.api.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Represents the inventory within a handheld radio.
 */
public interface IVHFHandheldCapability {

    /**
     * @return The item (battery) held by this handheld radio.
     */
    ItemStack getBattery();

    void setBattery(ItemStack item);

    int getFrequencyKiloHertz();

    void setFrequencyKiloHertz(int frequencyKiloHertz);

    boolean isPowered();

    void setPowered(boolean value);

    boolean isPTTDown();

    void setPTTDown(boolean value);

}
