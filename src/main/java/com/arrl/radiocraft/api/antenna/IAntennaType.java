package com.arrl.radiocraft.api.antenna;

import com.arrl.radiocraft.common.radio.antenna.Antenna;
import com.arrl.radiocraft.common.radio.antenna.AntennaData;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetworkPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public interface IAntennaType<T extends AntennaData> {

	ResourceLocation getId();

	/**
	 * Attempt to match this antenna type at level, pos.
	 * @return The details of the matching antenna, otherwise null if no match is found.
	 */
	Antenna<T> match(Level level, BlockPos pos);

	/**
	 * Apply the strength multiplier for transmitting to a given destination.
	 */
	void applyTransmitStrength(AntennaNetworkPacket packet, T data, BlockPos destination);

	/**
	 * Apply the strength multiplier for receiving from a given source.
	 */
	void applyReceiveStrength(AntennaNetworkPacket packet, T data, BlockPos pos);

	/**
	 * Get a default instance of data class, usually used for loading.
	 */
	T getDefaultData();

}
