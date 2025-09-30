package com.arrl.radiocraft.api.antenna;

import com.arrl.radiocraft.common.radio.antenna.AntennaCWPacket;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import net.minecraft.server.level.ServerLevel;

/**
 * An object representing data being sent between two {@link StaticAntenna}s.
 *
 * <p>See {@link AntennaCWPacket} for an example implementation of {@link IAntennaPacket}</p>
 */
public interface IAntennaPacket {

	/**
     * The wavelength of the signal being transmitted.
	 * @return The wavelength of the signal being transmitted.
	 */
	int getWavelength();

	/**
     * The frequency of the signal being transmitted.
	 * @return The frequency of the signal being transmitted.
	 */
	float getFrequency();

	/**
     * The strength of the signal.
	 * @return A 0-1 double value representing the strength of the signal.
	 */
	double getStrength();

	/**
     * The {@link IAntenna} the signal originated from.
	 * @return The {@link IAntenna} the signal originated from.
	 */
	IAntenna getSource();

	/**
     * The {@link ServerLevel} this packet is being sent within.
	 * @return The {@link ServerLevel} this packet is being sent within.
	 */
	ServerLevel getLevel();

}
