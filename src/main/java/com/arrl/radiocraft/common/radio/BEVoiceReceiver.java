package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.voice.RadiocraftVoicePlugin;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;

import java.util.UUID;

/**
 * {@link BEVoiceReceiver} is used for interacting with the Simple Voice Chat API to send sound packets being received by a Radio.
 * PCM audio gets re-encoded here and sent on an {@link AudioChannel}.
 */
public class BEVoiceReceiver implements IVoiceReceiver {

	private LocationalAudioChannel receiveChannel = null;
	private final int x;
	private final int y;
	private final int z;

	private boolean isReceiving;
	private static boolean warnedNoApi = false;

	public BEVoiceReceiver(int x, int y, int z) {
		this(x, y, z, false);
	}

	public BEVoiceReceiver(int x, int y, int z, boolean isReceiving) {
		this.isReceiving = isReceiving;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void openChannel(ServerLevel level) {
		receiveChannel = RadiocraftVoicePlugin.API.createLocationalAudioChannel(UUID.randomUUID(), level, RadiocraftVoicePlugin.API.createPosition(x, y, z));
		if(receiveChannel != null)
			receiveChannel.setDistance(16.0F);
	}

	/**
	 * Plays the packet's audio at the radio, scaled by the packet's strength. Does nothing when the voice chat server
	 * API is not available (no voice chat server running, e.g. a GameTest server), so the rest of the radio path can
	 * still be exercised without it.
	 */
	public void receive(AntennaVoicePacket antennaPacket) {
		if(!isReceiving)
			return;
		if(RadiocraftVoicePlugin.API == null) {
			if(!warnedNoApi) {
				warnedNoApi = true;
				Radiocraft.LOGGER.warn("Radio at {},{},{} received audio but the voice chat server API is not available; audio will not be played.", x, y, z);
			}
			return;
		}

		if(receiveChannel == null)
			openChannel(RadiocraftVoicePlugin.API.fromServerLevel(antennaPacket.getLevel()));
		if(receiveChannel == null)
			return;

		short[] rawAudio = antennaPacket.getRawAudio();
		for(int i = 0; i < rawAudio.length; i++)
			rawAudio[i] = (short)Math.round(rawAudio[i] * antennaPacket.getStrength()); // Apply appropriate gain for signal strength

		byte[] opusAudio = RadiocraftVoicePlugin.encodingManager.getOrCreate(antennaPacket.getSourcePlayer()).getEncoder().encode(rawAudio);
		receiveChannel.send(opusAudio);
	}

	public boolean isReceiving() {
		return isReceiving;
	}

	public void setReceiving(boolean value) {
		isReceiving = value;
	}

}
