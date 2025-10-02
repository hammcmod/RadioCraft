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
 * @param minFrequency The minimum frequency of the band in kHz
 * @param maxFrequency The maximum frequency of the band in kHz
 */
public record Band(int wavelength, int losRange, int minSkipDay, int maxSkipDay, int minSkipNight, int maxSkipNight, int minFrequency, int maxFrequency) {

    public static Band getBand(int wavelength) {
        RadiocraftServerConfig.BandConfig bandConfig = RadiocraftServerConfig.BAND_CONFIGS.get(wavelength);
        if(bandConfig == null) return null;
        return bandConfig.getBand();
    }

    //used to init the defaults in RadiocraftServerConfig
    public static Collection<Band> getDefaults(){
        return List.of(
                new Band(2,500,0,0,0,0,144000,148000),
                new Band(10,800,0,0,0,0,28000,29700),
                new Band(20,300,2000,6000,800,1000,14000,14350),
                new Band(40,900,1000,1100,100,3000,7300,9000),
                new Band(80,600,700,800,600,2000,3500,4000)
        );
    }

}
