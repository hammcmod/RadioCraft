package com.arrl.radiocraft.common.radio.voice;

import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EncodingManager {

	private final Map<UUID, EncodingData> encodingData = new HashMap<>();

	public EncodingData getOrCreate(UUID player) {
		if(!encodingData.containsKey(player))
			encodingData.put(player, new EncodingData());
		return encodingData.get(player);
	}

	public void close(UUID player) {
		if(encodingData.containsKey(player)) {
			encodingData.get(player).close();
			encodingData.remove(player);
		}
	}

	public static class EncodingData {

		private final OpusEncoder encoder;
		private final OpusDecoder decoder;

		public EncodingData() {
			if(RadiocraftVoicePlugin.API == null)
				throw new IllegalStateException("Tried to create EncodingData object without a valid API");

			encoder = RadiocraftVoicePlugin.API.createEncoder();
			decoder = RadiocraftVoicePlugin.API.createDecoder();
		}

		public OpusEncoder getEncoder() {
			return encoder;
		}

		public OpusDecoder getDecoder() {
			return decoder;
		}

		public void close() {
			encoder.close();
			decoder.close();
		}

		public void reset() {
			encoder.resetState();
			decoder.resetState();
		}

	}

}