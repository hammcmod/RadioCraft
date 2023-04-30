package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.voice.AntennaNetworkPacket;
import com.arrl.radiocraft.common.radio.voice.RadiocraftVoicePlugin;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;

import java.util.UUID;

/**
 * Represents a radio block entity
 */
public class Radio {

	private LocationalAudioChannel receiveChannel = null;

	private boolean isTransmitting;
	private boolean isReceiving;

	public Radio() {
		this(false, false);
	}

	public Radio(boolean isReceiving, boolean isTransmitting) {
		this.isTransmitting = isTransmitting;
		this.isReceiving = isReceiving;
	}

	public void openChannel(ServerLevel level, int x, int y, int z) {
		if(RadiocraftVoicePlugin.api == null)
			Radiocraft.LOGGER.error("Radiocraft VoiceChatServerApi is null.");
		receiveChannel = RadiocraftVoicePlugin.api.createLocationalAudioChannel(UUID.randomUUID(), level, RadiocraftVoicePlugin.api.createPosition(x, y, z));
	}

	public LocationalAudioChannel getReceiveChannel() {
		return receiveChannel;
	}

	public void receive(AntennaNetworkPacket antennaPacket) {
		if(RadiocraftVoicePlugin.encoder == null)
			Radiocraft.LOGGER.error("Radiocraft encoder is null.");
		else {
			RadiocraftVoicePlugin.encoder.resetState();
			byte[] opusAudio = RadiocraftVoicePlugin.encoder.encode(antennaPacket.getRawAudio());
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
