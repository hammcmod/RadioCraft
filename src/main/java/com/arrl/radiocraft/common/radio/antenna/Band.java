package com.arrl.radiocraft.common.radio.antenna;

/**
 * Defines the properties of a band. los = Line of Sight.
 */
public record Band(int wavelength, int losRange, int minSkipDay, int maxSkipDay, int minSkipNight, int maxSkipNight, int minFrequency, int maxFrequency) {}
