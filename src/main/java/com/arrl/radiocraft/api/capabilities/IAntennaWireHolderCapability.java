package com.arrl.radiocraft.api.capabilities;

import com.arrl.radiocraft.common.entities.AntennaWire;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

/**
 * Represents a {@link Player} capable of holding the end of an {@link AntennaWire}
 */
public interface IAntennaWireHolderCapability {

	/**
     * Get the {@link BlockPos} containing the held {@link AntennaWire}.
     * @param player The {@link Player} holding the {@link AntennaWire}
	 * @return The {@link BlockPos} containing the held {@link AntennaWire}
	 */
	BlockPos getHeldPos(Player player);

    /**
     * Set the {@link BlockPos} containing the held {@link AntennaWire}.
     * @param player The {@link Player} holding the {@link AntennaWire}
     * @param pos The {@link BlockPos} containing the held {@link AntennaWire}
     */
	void setHeldPos(Player player, BlockPos pos);
}
