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
     * Stores the RMS strength of the audio received in the last tick, updated every tick by {@link com.arrl.radiocraft.common.radio.voice.handheld.PlayerRadio}
     * @param rec new strength value
     */
    void setReceiveStrength(float rec);

    /**
     * Gets the RMS strength of the audio received in the last tick, updated every tick by {@link com.arrl.radiocraft.common.radio.voice.handheld.PlayerRadio} <br>
     * The decibels level of the receive strength can be calculated by <code>20.0f * log10(RMS / reference value)</code>
     * where the reference value is what the RMS of a 1 decibel sample should be. Used for the receive strength meter in {@link com.arrl.radiocraft.client.screens.radios.VHFHandheldScreen}
     * @return RMS receive strength
     */
    float getReceiveStrength();

}
