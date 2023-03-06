package com.arrl.radiocraft.common.radio;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Radio {

	private final List<LocationalAudioChannel> openChannels = new ArrayList<>();
	private boolean isTransmitting = false;
	private boolean isReceiving = false;

	public Radio() {
	}

	/**
	 * Opens a locational audio channel for VoiP at the given position.
	 * @param api SVC server api object
	 * @return channel opened, returns existing channel if location is already used.
	 */
	public LocationalAudioChannel openChannel(VoicechatServerApi api, ServerLevel level, int x, int y, int z) {
		for(LocationalAudioChannel channel : openChannels) {
			if(channelMatches(channel, x, y, z))
				return channel; // Fetch existing channel
		}

		LocationalAudioChannel newChannel = api.createLocationalAudioChannel(UUID.randomUUID(), level, api.createPosition(x, y, z));
		openChannels.add(newChannel);
		return newChannel;
	}

	/**
	 * Closes locational channels for this radio at a given position.
	 */
	public void closeChannel(int x, int y, int z) {
		openChannels.removeIf(channel -> channelMatches(channel, x, y, z));
	}

	/**
	 * Closes all locational channels used by this radio.
	 */
	public void closeAllChannels() {
		openChannels.clear();
	}

	public void send(MicrophonePacket packet) {
		for(LocationalAudioChannel channel : openChannels)
			channel.send(packet);
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

	private static boolean channelMatches(LocationalAudioChannel channel, int x, int y, int z) {
		Position pos = channel.getLocation();
		return (int)pos.getX() == x && (int)pos.getY() == y && (int)pos.getZ() == z;
	}

}
