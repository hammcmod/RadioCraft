package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.antenna.IAntennaNetwork;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AntennaNetwork implements IAntennaNetwork {

	private final Set<IAntenna> antennas = Collections.synchronizedSet(new HashSet<>()); // Synchronized list as it is read by the VoiP thread
	//TODO remove when confirmed seeing the old implementation inline is not useful for reference
//	private final Level level;

	public AntennaNetwork() {
//		this.level = level;
	}

	@Override
	public IAntenna addAntenna(IAntenna antenna) {
		antennas.add(antenna);
		return antenna;
	}

	@Override
	public void removeAntenna(IAntenna antenna) {
		antennas.remove(antenna);
	}

	@Override
	public Set<IAntenna> allAntennas() {
		return antennas;
	}

	//TODO remove when confirmed seeing the old implementation inline is not useful for reference
//
//	@Override
//	public Level getLevel() {
//		return level;
//	}

}