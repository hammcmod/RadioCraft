package com.arrl.radiocraft.api.antenna;

import com.arrl.radiocraft.common.radio.antenna.Antenna;
import com.arrl.radiocraft.common.radio.antenna.AntennaMorsePacket;
import net.minecraft.core.BlockPos;

/**
 * An object representing data being sent between two {@link Antenna}s.
 *
 * <p>See {@link AntennaMorsePacket} for an example implementation of {@link IAntennaPacket}</p>
 */
public interface IAntennaPacket {

	/**
	 * @return The wavelength of the signal being transmitted.
	 */
	int getWavelength();

	/**
	 * @return The frequency of the signal being transmitted.
	 */
	int getFrequency();

	/**
	 * @return A 0-1 double value representing the strength of the signal.
	 */
	double getStrength();

	/**
	 * @return The {@link BlockPos} the signal originated from.
	 */
	BlockPos getSource();

}
