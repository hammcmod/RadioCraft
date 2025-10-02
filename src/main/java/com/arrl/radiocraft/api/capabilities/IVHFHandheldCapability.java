package com.arrl.radiocraft.api.capabilities;

import net.minecraft.world.item.ItemStack;

/**
 * Represents the inventory within a handheld radio.
 */
public interface IVHFHandheldCapability {

    /**
     * Gets the item (battery) held by this handheld radio.
     * @return The item (battery) held by this handheld radio.
     */
    ItemStack getBattery();

    /**
     * Sets the item (battery) held by this handheld radio.
     * @param item The new item (battery) held by this handheld radio.
     */
    void setBattery(ItemStack item);

    /**
     * Gets the frequency of the handheld radio in kilohertz.
     * @return The frequency of the handheld radio in kilohertz.
     */
    int getFrequencyKiloHertz();

    /**
     * Sets the frequency of the handheld radio in kilohertz.
     * @param frequencyKiloHertz The new frequency in kilohertz.
     */
    void setFrequencyKiloHertz(int frequencyKiloHertz);

    /**
     * Gets the power state of the handheld radio.
     * @return The power state of the handheld radio.
     */
    boolean isPowered();

    /**
     * Sets the power state of the handheld radio.
     * @param value The new power state.
     */
    void setPowered(boolean value);

    /**
     * Gets the state of the PTT button.
     * @return The state of the PTT button.
     */
    boolean isPTTDown();

    /**
     * Sets the PTT button state.
     * @param value The new state of the PTT button.
     */
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

    /**
     * Gets the gain of the speaker.
     * @return The gain of the speaker (1.0 is 100% gain)
     */
    float getGain();

    /**
     * Sets the gain of the speaker.
     * @param gain The gain of the speaker (1.0 is 100% gain)
     */
    void setGain(float gain);

    /**
     * Gets the gain of the microphone.
     * @return The gain of the microphone (1.0 is 100% gain)
     */
    float getMicGain();

    /**
     * Sets the gain of the microphone.
     * @param micGain The gain of the microphone (1.0 is 100% gain)
     */
    void setMicGain(float micGain);
}
