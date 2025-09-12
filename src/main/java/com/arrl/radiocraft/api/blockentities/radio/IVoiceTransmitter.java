package com.arrl.radiocraft.api.blockentities.radio;

import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.common.radio.VoiceTransmitters;
import de.maxhenkel.voicechat.api.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Any object which transmits voice packets to an {@link IAntenna} should implement this. If this
 * {@link IVoiceTransmitter} also needs to listen to Simple Voice Chat packets, it should also register
 * itself to {@link VoiceTransmitters} when created and remove itself when destroyed.
 */
public interface IVoiceTransmitter {

	/**
	 * Process voice packet and broadcast to other radios. Called from voice thread.
	 *
	 * @param level level object (SVC API) for
	 * @param rawAudio raw PCM audio being sent in the packet.
     * @param sourcePlayer UUID of the player who sent the audio. Used for opus encoding/decoding.
	 */
	void acceptVoicePacket(ServerLevel level, short[] rawAudio, UUID sourcePlayer);

	/**
     * Check if this transmitter is currently open to transmit voice.
	 * @return True if this transmitter is currently open to transmit voice. Checks for if SSB mode is enabled, power is
 	 * on etc. should be done here.
	 */
	boolean canTransmitVoice();

	/**
     * Get the position of this {@link IVoiceTransmitter}.
	 * @return The position of this {@link IVoiceTransmitter}, used to determine if this transmitter is
	 * in range of the source of the voice packets.
	 */
	Vec3 getPos();

}
