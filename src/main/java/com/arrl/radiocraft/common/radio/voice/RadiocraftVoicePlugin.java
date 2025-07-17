package com.arrl.radiocraft.common.radio.voice;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.blockentities.radio.IVoiceTransmitter;
import com.arrl.radiocraft.common.radio.VoiceTransmitters;
import com.arrl.radiocraft.common.radio.voice.EncodingManager.EncodingData;
import com.arrl.radiocraft.common.radio.voice.handheld.PlayerRadioManager;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

@ForgeVoicechatPlugin
public class RadiocraftVoicePlugin implements VoicechatPlugin {

	public static VoicechatServerApi API = null;
	public static EncodingManager encodingManager = new EncodingManager();

	//TODO not currently working, will be used by audiochannels such as those in PlayerRadio
//	public static VolumeCategory handheldRadiosVolumeCategory;

	@Override
	public String getPluginId() {
		return Radiocraft.MOD_ID;
	}

	@Override
	public void registerEvents(EventRegistration registration) {
		registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket, 0);
		registration.registerEvent(PlayerDisconnectedEvent.class, this::onPlayerDisconnected, 0);
		registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStart, 0);
	}

	public void onServerStart(VoicechatServerStartedEvent event){
		//TODO not currently working
//		handheldRadiosVolumeCategory = event.getVoicechat().volumeCategoryBuilder()
//				.setId("radiocraft_radios")
//				.setName("Radios")
//				.setDescription("Volume for Radios")
//				.build();
//		event.getVoicechat().registerVolumeCategory(handheldRadiosVolumeCategory);
	}

	public void onMicrophonePacket(MicrophonePacketEvent event) { // This should only be called serverside
		if(API == null)
			API = event.getVoicechat();

		VoicechatConnection connection = event.getSenderConnection();

		if(connection == null) return; //the voicechat api is written such that the server could technically generate microphone packets (not specific to a player), but they wouldn't have a location so we ignore. Could implement something in the future

		de.maxhenkel.voicechat.api.ServerPlayer sender = connection.getPlayer();

		if(sender.getPlayer() instanceof ServerPlayer player) {

			// Decode voice packet
			EncodingData encodingData = encodingManager.getOrCreate(sender.getUuid());
			byte[] encodedAudio = event.getPacket().getOpusEncodedData();
			short[] decodedAudio = encodingData.getDecoder().decode(encodedAudio);
			if(encodedAudio.length == 0)
				encodingData.reset();
			else {
				PlayerRadioManager.get(sender.getUuid()).ifPresent(playerRadio -> {
					//still need to reconsider the structure for static antennas so not deleting this just yet, but for synchronization the check is moved inside
//					if (playerRadio.canTransmitVoice()){ //TODO reconsider this structure, should the onus be on the radio to know if it should transmit instead?
						playerRadio.acceptVoicePacket(sender.getServerLevel(), decodedAudio, sender.getUuid());
//					}
				});

				//this is the auditory range NOT the radio range
				double sqrRange = API.getBroadcastRange();
				sqrRange *= sqrRange;

				List<IVoiceTransmitter> listeningMics = VoiceTransmitters.LISTENERS.get((Level) sender.getServerLevel().getServerLevel());

				if(listeningMics != null) for (IVoiceTransmitter listener : listeningMics) { // All radios in audible range of the sender will receive the packet
					Vec3 pos = listener.getPos();
					Position playerPos = sender.getPosition();

					if (pos.distanceToSqr(new Vec3(playerPos.getX(), playerPos.getY(), playerPos.getZ())) > sqrRange)
						continue; // Do not transmit if out of range.

					if (!listener.canTransmitVoice())
						continue; // Do not transmit if listener is not accepting packets.

					listener.acceptVoicePacket(sender.getServerLevel(), decodedAudio, sender.getUuid());
				}
			}
		}
	}

	public void onPlayerDisconnected(PlayerDisconnectedEvent event) {
		encodingManager.close(event.getPlayerUuid());
	}

}
