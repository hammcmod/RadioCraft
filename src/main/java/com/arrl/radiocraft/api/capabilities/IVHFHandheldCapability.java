package com.arrl.radiocraft.api.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Represents the inventory within a handheld radio.
 */
public interface IVHFHandheldCapability extends INBTSerializable<CompoundTag> {

    /**
     * @return The item (battery) held by this handheld radio.
     */
    ItemStack getItem();

    void setItem(ItemStack item);

    int getFrequency();

    void setFrequency(int frequency);

    boolean isPowered();

    void setPowered(boolean value);

    boolean isPTTDown();

    void setPTTDown(boolean value);

}
