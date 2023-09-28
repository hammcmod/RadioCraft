package com.arrl.radiocraft.api.antenna;

import com.arrl.radiocraft.common.radio.antenna.BEAntenna;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Represents a network of compatible antennas which can share signals with each other. By default, there is a network
 * for HF and VHF radios on every {@link Level}
 */
public interface IAntennaNetwork {

	/**
	 * IMPORTANT: Do not call this from the VoiP thread
	 *
	 * <p>Adds an {@link BEAntenna} to the network.</p>
	 *
	 * @param antenna The {@link BEAntenna} to be added.
	 *
	 * @returns The {@link BEAntenna} which was added.
	 */
	IAntenna addAntenna(IAntenna antenna);

	/**
	 * IMPORTANT: Do not call this from the VoiP thread
	 *
	 * <p>Removes an {@link BEAntenna} from the network.</p>
	 *
	 * @param antenna The {@link BEAntenna} to be removed.
	 */
	void removeAntenna(IAntenna antenna);

	/**
	 * @return A list of all {@link BEAntenna}s present on the network.
	 */
	List<IAntenna> allAntennas();

	/**
	 * @return The {@link Level} this network is contained within.
	 */
	Level getLevel();

}
