package com.arrl.radiocraft.api.antenna;

import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * Represents a network of compatible antennas which can share signals with each other. By default, there is a network
 * for HF and VHF radios on every {@link Level}
 */
public interface IAntennaNetwork {

	/**
	 * IMPORTANT: Do not call this from the VoiP thread
	 *
	 * <p>Adds an {@link StaticAntenna} to the network.</p>
	 *
	 * @param antenna The {@link StaticAntenna} to be added.
	 *
	 * @returns The {@link StaticAntenna} which was added.
	 */
	IAntenna addAntenna(IAntenna antenna);

	/**
	 * IMPORTANT: Do not call this from the VoiP thread
	 *
	 * <p>Removes an {@link StaticAntenna} from the network.</p>
	 *
	 * @param antenna The {@link StaticAntenna} to be removed.
	 */
	void removeAntenna(IAntenna antenna);

	/**
	 * @return A list of all {@link StaticAntenna}s present on the network.
	 */
	Set<IAntenna> allAntennas();

	/**
	 * @return The {@link Level} this network is contained within.
	 */
	Level getLevel();

}
