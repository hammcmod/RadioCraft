package com.arrl.radiocraft.api.capabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Block Entity Callsign Class
 * @param level The level of the block entity
 * @param pos The position of the block entity
 * @param callsign The callsign of the block entity
 * @param licenseClass The license of the block entity. These will generally be BROADCAST_* licenses.
 */
public record BlockEntityCallsignData(Level level, BlockPos pos, String callsign, LicenseClass licenseClass) {}
