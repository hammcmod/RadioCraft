package com.arrl.radiocraft.api.capabilities;

/**
 * Player Callsign Data Class
 * @param playerUUID The PlayerUUID of the license holder
 * @param callsign The Callsign of the player
 * @param licenseClass The License Class of the player. These will generally be TECHNICIAN, GENERAL, or AMATEUR_EXTRA.
 */
public record PlayerCallsignData(String playerUUID, String callsign, LicenseClass licenseClass) {}
