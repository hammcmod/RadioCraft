package com.arrl.radiocraft.api.blockentities.radio;

import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaCWPacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetwork;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import com.arrl.radiocraft.common.radio.morse.CWReceiveBuffer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Any {@link BlockEntity} which receives and plays back CW/morse should implement {@link ICWReceiver}.
 */
public interface ICWReceiver {

	/**
	 * @return The {@link CWReceiveBuffer} for this receiver, which is what receives and sorts {@link CWBuffer}s from
	 * link CWBufferPacket s on the client. TODO fix link/code
	 */
	CWReceiveBuffer getCWReceiveBuffer();

	/**
	 * @return True if this receiver is currently open to receive CW/morse. Checks for if CW mode is enabled, power is
	 * on etc. should be done here.
	 */
	boolean canReceiveCW();

	/**
	 * Handles the (server-side) receiving of CW packets from an {@link AntennaNetwork}. This should also be where any
	 * link CWBufferPacket s are then sent to clients tracking this receiver. TODO fix link/code
	 *
	 * @param packet The {@link AntennaCWPacket} being received. This is an {@link IAntennaPacket}, not a network packet.
	 */
	void receiveCW(AntennaCWPacket packet);

}
