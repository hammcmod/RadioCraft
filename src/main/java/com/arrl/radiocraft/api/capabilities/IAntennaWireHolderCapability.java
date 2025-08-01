package com.arrl.radiocraft.api.capabilities;

import com.arrl.radiocraft.common.entities.AntennaWire;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

/**
 * Represents a {@link Player} capable of holding the end of an {@link AntennaWire}
 */
public interface IAntennaWireHolderCapability {

	/**
	 * @return The {@link BlockPos} containing the held {@link AntennaWire}
	 */
	BlockPos getHeldPos(Player player);

	void setHeldPos(Player player, BlockPos pos);
}
