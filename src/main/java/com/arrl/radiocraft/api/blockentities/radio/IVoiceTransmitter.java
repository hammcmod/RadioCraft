package com.arrl.radiocraft.api.blockentities.radio;

import com.arrl.radiocraft.api.antenna.IAntenna;
import de.maxhenkel.voicechat.api.ServerLevel;

import java.util.UUID;

/**
 * Any object which transmits voice packets to an {@link IAntenna} should implement this.
 */
public interface IVoiceTransmitter {

	/**
	 * Process voice packet and broadcast to other radios. Called from voice thread.
	 *
	 * @param level level object (SVC API) for
	 *
	 */
	void acceptVoicePacket(ServerLevel level, short[] rawAudio, UUID sourcePlayer);

	/**
	 * @return True if this transmitter is currently open to transmit voice. Checks for if SSB mode is enabled, power is
 	 * on etc. should be done here.
	 */
	boolean canTransmitVoice();

}
