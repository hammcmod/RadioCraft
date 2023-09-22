package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.common.benetworks.power.ConnectionType;
import com.arrl.radiocraft.common.benetworks.power.PowerNetwork;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Represents a {@link BlockEntity} which interacts with one or more {@link PowerNetwork}s.
 */
public interface IPowerNetworkItem extends IBENetworkItem {

	/**
	 * The type of relationship this {@link BlockEntity} has with attached {@link PowerNetwork}s
	 * @return {@link ConnectionType#PULL} if this BE pulls power FROM the network, {@link ConnectionType#PUSH} if this
	 * BE pushes power TO the network or {@link ConnectionType#NO_INTERACT} if this BE has special behaviour which needs
	 * to be ignored by methods in {@link PowerNetwork}.
	 */
	 default ConnectionType getConnectionType() {
		 return ConnectionType.PULL;
	 }

}
