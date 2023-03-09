package com.arrl.radiocraft.common.radio;

import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Radio {

	private final Map<BlockPos, Integer> connections = new HashMap<>();
	private LocationalAudioChannel receiveChannel = null;

	private boolean isTransmitting = true;
	private boolean isReceiving = true;

	public Radio() {
	}

	public void openChannel(VoicechatServerApi api, ServerLevel level, int x, int y, int z) {
		receiveChannel = api.createLocationalAudioChannel(UUID.randomUUID(), level, api.createPosition(x, y, z));
	}

	public LocationalAudioChannel getReceiveChannel() {
		return receiveChannel;
	}

	public void receive(MicrophonePacket packet, int quality) {
		receiveChannel.send(packet);
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

	public Map<BlockPos, Integer> getConnections() {
		return connections;
	}

}
