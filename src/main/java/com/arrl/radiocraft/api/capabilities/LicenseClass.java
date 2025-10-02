package com.arrl.radiocraft.api.capabilities;

/**
 * The different types of licenses that can be applied to a block entity or player.
 */
public enum LicenseClass {
    /**
     * Technician License (FCC)
     */
    TECHNICIAN,
    /**
     * General License (FCC)
     */
    GENERAL,
    /**
     * Amateur Extra License (FCC)
     */
    AMATEUR_EXTRA,
    /**
     * Weather Broadcasts (Like NOAA weather radio)
     */
    BROADCAST_WEATHER,
    /**
     * Radio Broadcasts (Like AM/FM stations that play music or news)
     */
    BROADCAST_RADIO,
    /**
     * Television Broadcasts (Like Analog, Digital, or Satellite TV)
     */
    BROADCAST_TELEVISION,
    /**
     * Wireless Internet Service Provider (Like Verizon) including microwave dish P2P's
     * or Satellite Internet Service Provider (Like Starlink)
     */
    WIRELESS_INTERNET_SERVICE_PROVIDER
}
