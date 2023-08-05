package com.arrl.radiocraft.common.radio.voice;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.AbstractRadioBlockEntity;
import com.arrl.radiocraft.common.radio.antenna.RadioManager;
import com.arrl.radiocraft.common.radio.voice.EncodingManager.EncodingData;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;

@ForgeVoicechatPlugin
public class RadiocraftVoicePlugin implements VoicechatPlugin {

	public static VoicechatServerApi api = null;
	public static EncodingManager encodingManager = new EncodingManager();

	@Override
	public String getPluginId() {
		return Radiocraft.MOD_ID;
	}

	@Override
	public void registerEvents(EventRegistration registration) {
		registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket, 0);
		registration.registerEvent(PlayerDisconnectedEvent.class, this::onPlayerDisconnected, 0);
	}

	public void onMicrophonePacket(MicrophonePacketEvent event) { // This should only be called serverside
		if(api == null)
			api = event.getVoicechat();

		de.maxhenkel.voicechat.api.ServerPlayer sender = event.getSenderConnection().getPlayer();

		if(sender.getPlayer() instanceof ServerPlayer player) {
			double sqrRange = api.getBroadcastRange();
			sqrRange *= sqrRange;

			List<AbstractRadioBlockEntity> radios = RadioManager.RADIOS.get(player.getLevel());

			for(AbstractRadioBlockEntity radio : radios) { // All radios in range of the sender will receive the packet
				BlockPos pos = radio.getBlockPos();
				if(pos.distToCenterSqr(player.position()) < sqrRange) {
					BlockEntity blockEntity = player.getLevel().getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
					if(blockEntity instanceof AbstractRadioBlockEntity be && be.isPTTDown()) {
						EncodingData encodingData = encodingManager.getOrCreate(sender.getUuid());

						byte[] encodedAudio = event.getPacket().getOpusEncodedData();
						if(encodedAudio.length == 0)
							encodingData.reset();
						else
							be.acceptVoicePacket(sender.getServerLevel(), encodingData.getDecoder().decode(encodedAudio), sender.getUuid());
					}
				}
			}
		}
	}

	public void onPlayerDisconnected(PlayerDisconnectedEvent event) {
		encodingManager.close(event.getPlayerUuid());
	}

}
