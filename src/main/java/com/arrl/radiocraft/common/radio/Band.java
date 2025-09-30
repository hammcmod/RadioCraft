package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Defines the properties of a band
 * @param name The name of the band (name in m/cm)
 * @param losRange The range of the band in meters (blocks)
 * @param minSkipDay The minimum distance to skip during daytime in meters (blocks)
 * @param maxSkipDay The maximum distance to skip during daytime in meters (blocks)
 * @param minSkipNight The minimum distance to skip during nighttime in meters (blocks)
 * @param maxSkipNight The maximum distance to skip during nighttime in meters (blocks)
 * @param minFrequency The minimum frequency of the band in Hz
 * @param maxFrequency The maximum frequency of the band in Hz
 */
public record Band(String name, int losRange, int minSkipDay, int maxSkipDay, int minSkipNight, int maxSkipNight, float minFrequency, float maxFrequency) {

    /**
     * Get the band for a given "named" wavelength.
     * @param wavelength A whole wavelength in m
     * @return Band instance or null if not found
     */
    public static Band getBand(int wavelength) {
        RadiocraftServerConfig.BandConfig bandConfig = RadiocraftServerConfig.BAND_CONFIGS.get(wavelength + "m");
        if(bandConfig == null) return null;
        return bandConfig.getBand();
    }

    /**
     * Get the band for a given frequency in Hz.
     * @param frequencyHertz The frequency in Hz
     * @return Band instance or null if there is not a band that contains this frequency.
     */
    public static Band getBand(float frequencyHertz) {
        try {
            List<Map.Entry<String, RadiocraftServerConfig.BandConfig>> list = RadiocraftServerConfig.BAND_CONFIGS.entrySet().stream()
                    .filter(entry -> entry.getValue().getBand().minFrequency() <= frequencyHertz && frequencyHertz <= entry.getValue().getBand().maxFrequency()).toList();
            return list.getFirst().getValue().getBand();
        } catch (Exception e) {
            Radiocraft.LOGGER.error("Frequency {} not found in band config", frequencyHertz);
            return null;
        }
    }

    /**
     * Get the band for a given name.
     * @param name The name of the band (name in m)
     * @return Band instance or null if not found
     */
    public static Band getBand(String name) {
        RadiocraftServerConfig.BandConfig bandConfig = RadiocraftServerConfig.BAND_CONFIGS.get(name);
        if(bandConfig == null) return null;
        return bandConfig.getBand();
    }

    //used to init the defaults in RadiocraftServerConfig
    public static Collection<Band> getDefaults(){
        return List.of(
                new Band("2m",500,0,0,0,0,144_000_000,148_000_000),
                new Band("10m",800,0,0,0,0,28_000_000,29_700_000),
                new Band("20m",300,2000,6000,800,1000,14_000_000,14_350_000),
                new Band("40m",900,1000,1100,100,3000,7_300_000,9_000_000),
                new Band("80m",600,700,800,600,2000,3_500_000,4_000_000)
        );
    }

}
