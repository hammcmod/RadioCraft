package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.RadiocraftServerConfig;

import java.util.Collection;
import java.util.List;

/**
 * Defines the properties of a band
 * @param wavelength The wavelength of the band in Hz.
 * @param losRange The range of the band in meters (blocks)
 * @param minSkipDay The minimum distance to skip during daytime in meters (blocks)
 * @param maxSkipDay The maximum distance to skip during daytime in meters (blocks)
 * @param minSkipNight The minimum distance to skip during nighttime in meters (blocks)
 * @param maxSkipNight The maximum distance to skip during nighttime in meters (blocks)
 * @param minFrequency The minimum frequency of the band in Hz
 * @param maxFrequency The maximum frequency of the band in Hz
 */
public record Band(int wavelength, int losRange, int minSkipDay, int maxSkipDay, int minSkipNight, int maxSkipNight, float minFrequency, float maxFrequency) {

    public static Band getBand(int wavelength) {
        RadiocraftServerConfig.BandConfig bandConfig = RadiocraftServerConfig.BAND_CONFIGS.get(wavelength);
        if(bandConfig == null) return null;
        return bandConfig.getBand();
    }

    //used to init the defaults in RadiocraftServerConfig
    public static Collection<Band> getDefaults(){
        return List.of(
                new Band(2,500,0,0,0,0,144_000_000,148_000_000),
                new Band(10,800,0,0,0,0,28_000_000,29_700_000),
                new Band(20,300,2000,6000,800,1000,14_000_000,14_350_000),
                new Band(40,900,1000,1100,100,3000,7_300_000,9_000_000),
                new Band(80,600,700,800,600,2000,3_500_000,4_000_000)
        );
    }

}
