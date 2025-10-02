package com.arrl.radiocraft.api.capabilities;

import net.minecraft.core.GlobalPos;

/**
 * Block Entity Callsign Class
 * @param pos The position of the block entity
 * @param callsign The callsign of the block entity
 * @param licenseClass The license of the block entity. These will generally be BROADCAST_* licenses.
 */
public record BlockEntityCallsignData(GlobalPos pos, String callsign, LicenseClass licenseClass) {}
