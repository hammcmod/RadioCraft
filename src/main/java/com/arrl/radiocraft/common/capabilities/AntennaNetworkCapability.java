package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.api.capabilities.IAntennaNetworkCapability;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetwork;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class AntennaNetworkCapability implements IAntennaNetworkCapability {

	private final Map<ResourceLocation, AntennaNetwork> networks = new HashMap<>();

	@Override
	public AntennaNetwork getNetwork(ResourceLocation id) {
		return networks.get(id);
	}

	@Override
	public AntennaNetwork setNetwork(ResourceLocation id, AntennaNetwork network) {
		networks.put(id, network);
		return network;
	}

}
