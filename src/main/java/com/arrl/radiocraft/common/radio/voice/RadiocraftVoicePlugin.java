package com.arrl.radiocraft.common.radio.voice;

import com.arrl.radiocraft.Radiocraft;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;

@ForgeVoicechatPlugin
public class RadiocraftVoicePlugin implements VoicechatPlugin {

	@Override
	public String getPluginId() {
		return Radiocraft.MOD_ID;
	}

	@Override
	public void registerEvents(EventRegistration registration) {
		registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket, 0);
	}

	public void onMicrophonePacket(MicrophonePacketEvent event) {

	}

}
