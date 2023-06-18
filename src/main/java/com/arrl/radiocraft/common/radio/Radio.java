package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetworkPacket;
import com.arrl.radiocraft.common.radio.voice.RadiocraftVoicePlugin;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;

import java.util.UUID;

/**
 * Represents a radio block entity
 */
public class Radio {

	private LocationalAudioChannel receiveChannel = null;
	private final int x;
	private final int y;
	private final int z;

	private boolean isTransmitting;
	private boolean isReceiving;

	public Radio(int x, int y, int z) {
		this(x, y, z, false, false);
	}

	public Radio(int x, int y, int z, boolean isReceiving, boolean isTransmitting) {
		this.isTransmitting = isTransmitting;
		this.isReceiving = isReceiving;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void openChannel(ServerLevel level) {
		if(RadiocraftVoicePlugin.api == null)
			Radiocraft.LOGGER.error("Radiocraft VoiceChatServerApi is null.");
		receiveChannel = RadiocraftVoicePlugin.api.createLocationalAudioChannel(UUID.randomUUID(), level, RadiocraftVoicePlugin.api.createPosition(x, y, z));
	}

	public void receive(AntennaNetworkPacket antennaPacket) {
		if(isReceiving) {
			if(receiveChannel == null)
				openChannel(antennaPacket.getLevel());

			short[] rawAudio = antennaPacket.getRawAudio();
			for(int i = 0; i < rawAudio.length; i++)
				rawAudio[i] = (short)Math.round(rawAudio[i] * antennaPacket.getStrength()); // Apply appropriate gain for signal strength

			byte[] opusAudio = RadiocraftVoicePlugin.encodingManager.getOrCreate(antennaPacket.getSourcePlayer()).getEncoder().encode(rawAudio);
			receiveChannel.send(opusAudio);
		}
	}

	public boolean isTransmitting() {
		return isTransmitting;
	}

	public boolean isReceiving() {
		return isReceiving;
	}

	public void setTransmitting(boolean value) {
		isTransmitting = value;
	}

	public void setReceiving(boolean value) {
		isReceiving = value;
	}

}
