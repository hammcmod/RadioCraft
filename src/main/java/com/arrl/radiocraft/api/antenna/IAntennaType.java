package com.arrl.radiocraft.api.antenna;

import com.arrl.radiocraft.common.radio.antenna.*;
import com.arrl.radiocraft.common.radio.antenna.types.DipoleAntennaType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * IMPORTANT: Every {@link IAntennaType} should be registered in {@link AntennaTypes} for the Radiocraft to know that it
 * exists.
 *
 * <p>{@link IAntennaType} represents the rules defining a specific type of antenna. This interface is what will be used to
 * check if an antenna can be counted as this type, save and load any data this {@link IAntennaType} needs as well as
 * calculate transmit and receive values.</p>
 *
 * <p>See {@link DipoleAntennaType} for an example implementation of both {@link IAntennaType} and {@link AntennaData}</p>
 *
 * @param <T> Data class used to save/load any instance specific data relating to this type of antenna (e.g. the length
 *           of a dipole antenna's arms).
 */
public interface IAntennaType<T extends AntennaData> {

	/**
	 * @return The {@link ResourceLocation} used to identify this {@link IAntennaType} within the registry, as well as
	 * save/load its data.
	 */
	ResourceLocation getId();

	/**
	 * Check if there is a valid antenna of this type at the specified position.
	 *
	 * @param level The {@link Level} containing the potential antenna.
	 * @param pos The {@link BlockPos} of the potential antenna within level.
	 *
	 * @return The matching antenna, otherwise null if no match is found.
	 */
	StaticAntenna<T> match(Level level, BlockPos pos);

	/**
	 * Get the strength multiplier for transmitting an {@link IAntennaPacket} to a given destination.
	 *
	 * @param packet The voice packet to be sent.
	 * @param data The {@link AntennaData} of the sending antenna.
	 * @param destination The position the {@link IAntennaPacket} is being sent to.
	 * @param isCW True if the packet is a CW packet (50% more range on most antennas), otherwise false.
	 *
	 * @return The strength multiplier to be applied to the given {@link IAntennaPacket}.
	 */
	double getTransmitEfficiency(IAntennaPacket packet, T data, BlockPos destination, boolean isCW);

	/**
	 * Calculate the strength multiplier for receiving a packet from a given position.
	 *
	 * @param packet The {@link IAntennaPacket} being received.
	 * @param data The {@link AntennaData} of the sending antenna.
	 * @param pos The position of the antenna which sent the {@link IAntennaPacket}.
	 *
	 * @return The strength multiplier to be applied to the given {@link IAntennaPacket}.
	 */
	default double getReceiveEfficiency(IAntennaPacket packet, T data, BlockPos pos) {
		return 0.0D;
	}

	/**
	 * Calculates the SWR of an antenna for a given wavelength based on it's properties. This has an
	 * impact on the efficiency of the antenna.
	 *
	 * @param data {@link AntennaData} object belonging to this antenna.
	 * @param wavelength The wavelength to be used for checking SWR.
	 *
	 * @return The SWR of the antenna while using the specified wavelength.
	 */
	double getSWR(T data, int wavelength);

	/**
	 * Get an {@link AntennaData} instance which represents the default starting values for this {@link IAntennaType}.
	 * Usually this will only be used to create an instance used while loading the {@link AntennaData} from a
	 * {@link BlockEntity}.
	 *
	 * @return An instance of {@link AntennaData} representing the default state of this {@link IAntennaType}.
	 */
	T getDefaultData();

}
