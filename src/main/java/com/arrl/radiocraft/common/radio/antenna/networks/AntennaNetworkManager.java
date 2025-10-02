package com.arrl.radiocraft.common.radio.antenna.networks;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.IAntennaNetworkCapability;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for grabbing antenna networks from a level, as well as creating
 * new ones if they are not present.
 */
public class AntennaNetworkManager {

	public static final ResourceLocation HF_ID = Radiocraft.id("hf");
	public static final ResourceLocation VHF_ID = Radiocraft.id("vhf");
	public static Map<ResourceLocation, AntennaNetwork> networks = new HashMap<>();

	static{
		networks.put(HF_ID, new AntennaNetwork());
		networks.put(VHF_ID, new AntennaNetwork());
	}

	/**
	 * Attempt to grab an existing {@link AntennaNetwork} from a {@link Level} by ID, or
	 * create one if there are none present on that {@link Level}.
	 *
	 * @param id The {@link ResourceLocation} ID of the desired {@link AntennaNetwork}.
	 *
	 * @return {@link AntennaNetwork} if one was found or created, otherwise null if level was
	 * missing {@link IAntennaNetworkCapability} for some reason.
	 */
	public static AntennaNetwork getNetwork(ResourceLocation id) {
		return networks.get(id);
	}

}
