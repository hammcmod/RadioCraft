package com.arrl.radiocraft.api.antenna;

import com.arrl.radiocraft.common.radio.antenna.Antenna;
import com.arrl.radiocraft.common.radio.voice.AntennaNetworkPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public interface IAntennaType<T> {

	ResourceLocation getId();

	/**
	 * Apply the strength multiplier for transmitting to a given destination.
	 */
	void applyTransmitStrength(AntennaNetworkPacket packet, T data, BlockPos pos, BlockPos destination);

	/**
	 * Apply the strength multiplier for receiving from a given source.
	 */
	void applyReceiveStrength(AntennaNetworkPacket packet, T data, BlockPos pos, BlockPos source);

	/**
	 * Attempt to match this antenna type at level, pos.
	 * @return The details of the matching antenna, otherwise null if no match is found.
	 */
	Antenna<T> match(Level level, BlockPos pos);

}
