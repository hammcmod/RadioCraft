package com.arrl.radiocraft.common.radio;

import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class AntennaManager {

	private static final Map<Level, AntennaNetwork> NETWORKS = new HashMap<>();

	public static AntennaNetwork getNetwork(Level level) {
		if(!NETWORKS.containsKey(level))
			setNetwork(level, new AntennaNetwork());
		return NETWORKS.get(level);
	}

	public static void setNetwork(Level level, AntennaNetwork network) {
		NETWORKS.put(level, network);
	}


}
