package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.voice.RadiocraftVoicePlugin;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * {@link HandheldVoiceReceiver} is used for interacting with the Simple Voice Chat API to send sound packets being received by a Radio.
 * PCM audio gets re-encoded here and sent on an {@link AudioChannel}.
 */
public class HandheldVoiceReceiver implements IVoiceReceiver {

	private EntityAudioChannel receiveChannel = null;
	private final Entity entity;

	private boolean isReceiving;

	public HandheldVoiceReceiver(Entity entity) {
		this(entity, false);
	}

	public HandheldVoiceReceiver(Entity entity, boolean isReceiving) {
		this.isReceiving = isReceiving;
		this.entity = entity;
	}

	public void openChannel() {
		if(RadiocraftVoicePlugin.API == null)
			Radiocraft.LOGGER.error("Radiocraft VoiceChatServerApi is null.");
		receiveChannel = RadiocraftVoicePlugin.API.createEntityAudioChannel(UUID.randomUUID(), RadiocraftVoicePlugin.API.fromEntity(entity));
	}

	public void receive(AntennaVoicePacket antennaPacket) {
		if(isReceiving) {
			if(receiveChannel == null)
				openChannel();

			short[] rawAudio = antennaPacket.getRawAudio();
			for(int i = 0; i < rawAudio.length; i++)
				rawAudio[i] = (short)Math.round(rawAudio[i] * antennaPacket.getStrength()); // Apply appropriate gain for signal strength

			byte[] opusAudio = RadiocraftVoicePlugin.encodingManager.getOrCreate(antennaPacket.getSourcePlayer()).getEncoder().encode(rawAudio);
			receiveChannel.send(opusAudio);
		}
	}

	public boolean isReceiving() {
		return isReceiving;
	}

	public void setReceiving(boolean value) {
		isReceiving = value;
	}

}