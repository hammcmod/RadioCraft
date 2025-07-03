package com.arrl.radiocraft.api.capabilities;

import net.minecraft.world.item.ItemStack;

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

    /**
     * Sets if the receiving indicator light should be on
     * @param rec true if the receive light should be on
     */
    void setReceiveIndicator(boolean rec);

    /**
     * Gets the status of the receiving light (on if we are currently receiving a transmission)
     * @return if the receive indicator should be on
     */
    boolean getReceiveIndicator();

}
