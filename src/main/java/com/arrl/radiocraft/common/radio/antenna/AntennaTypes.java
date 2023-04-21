package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.api.antenna.IAntennaType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

/**
 * Handles registration and matching of antennas.
 */
public class AntennaTypes {

	private static final ArrayList<IAntennaType<?>> ANTENNA_TYPES = new ArrayList<>();

	public <C extends IAntennaType<?>> C registerType(C type) {
		ANTENNA_TYPES.add(type);
		return type;
	}

	/**
	 * Get the first matching type and wavelength for an antenna.
	 * @return Match info for the type and wavelength of the antenna, otherwise null if no match is found.
	 */
	public static Antenna<?> match(Level level, BlockPos pos) {
		for(IAntennaType<?> type : ANTENNA_TYPES) {
			Antenna<?> antenna = type.match(level, pos);
			if(antenna != null)
				return antenna;
		}
		return null;
	}

}
