package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.api.antenna.IAntennaType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

/**
 * Handles registration and matching of antennas.
 */
public class AntennaTypes {

	private static final ArrayList<IAntennaType<?>> ANTENNA_TYPES = new ArrayList<>();

	public static <C extends IAntennaType<?>> C registerType(C type) {
		ANTENNA_TYPES.add(type);
		return type;
	}

	public static IAntennaType<?> getType(ResourceLocation id) {
		for(IAntennaType<?> type : ANTENNA_TYPES) {
			if(type.getId().equals(id))
				return type;
		}
		return null;
	}

	/**
	 * Get the first matching antenna at a position.
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
