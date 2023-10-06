package com.arrl.radiocraft.api.blockentities.radio;

import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetwork;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import com.arrl.radiocraft.common.radio.morse.CWSendBuffer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collection;

/**
 * Any {@link BlockEntity} which sends CW/morse to an {@link AntennaNetwork} should implement {@link IMorseTransmitter}.
 */
public interface IMorseTransmitter {

	/**
	 * @return The {@link CWSendBuffer} for this transmitter, which is what constructs {@link CWBuffer}s and sends
	 * them to the server. This is for client-side use.
	 */
	CWSendBuffer getCWSendBuffer();

	/**
	 * Handle the server-side sending of {@link CWBuffer}s. Usually forwards the buffers to an {@link IAntenna}.
	 * @param buffers The {@link CWBuffer}s to be sent, does not necessarily need to be in order as clients will
	 *                re-order them as received.
	 */
	void transmitMorse(Collection<CWBuffer> buffers);
}
