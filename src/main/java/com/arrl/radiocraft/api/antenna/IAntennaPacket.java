package com.arrl.radiocraft.api.antenna;

import com.arrl.radiocraft.common.radio.antenna.AntennaCWPacket;
import com.arrl.radiocraft.common.radio.antenna.BEAntenna;

/**
 * An object representing data being sent between two {@link BEAntenna}s.
 *
 * <p>See {@link AntennaCWPacket} for an example implementation of {@link IAntennaPacket}</p>
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
	 * @return The {@link IAntenna} the signal originated from.
	 */
	IAntenna getSource();

}
