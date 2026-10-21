package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.voice.RadioVoiceProcessor;
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
	private final RadioVoiceProcessor processor = new RadioVoiceProcessor(); // Only touched from the voice chat thread.
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

	/** Plays the packet's audio at the radio at its own strength, with no noise to sit against. */
	public void receive(AntennaVoicePacket antennaPacket) {
		receive(antennaPacket, antennaPacket.getStrength(), 0.0D);
	}

	/**
	 * Plays the packet's audio at the radio through the receiver's {@link RadioVoiceProcessor}. Does nothing when the
	 * voice chat server API is not available (no voice chat server running, e.g. a GameTest server), so the rest of the
	 * radio path can still be exercised without it.
	 *
	 * @param level Output level, see {@link com.arrl.radiocraft.common.radio.voice.ReceiverMix#voiceLevel}.
	 * @param rasp  Aurora rasp, 0 to 1.
	 */
	public void receive(AntennaVoicePacket antennaPacket, double level, double rasp) {
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

		short[] rawAudio = processor.process(antennaPacket.getRawAudio(), RadioVoiceProcessor.currentSettings(), level, rasp);
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
