package com.arrl.radiocraft.api.capabilities;

import com.arrl.radiocraft.common.radio.antenna.AntennaNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Capability attached to {@link Level} containing every {@link AntennaNetwork}
 * present within that {@link Level}.
 */
public interface IAntennaNetworkCapability {

    /**
     * @return {@link AntennaNetwork}, or null if none are found.
     */
    AntennaNetwork getNetwork(ResourceLocation id);

    /**
     * @return The network being added.
     */
    AntennaNetwork setNetwork(ResourceLocation id, AntennaNetwork network);

}
