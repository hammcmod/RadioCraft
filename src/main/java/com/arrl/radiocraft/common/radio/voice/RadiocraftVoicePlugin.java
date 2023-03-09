package com.arrl.radiocraft.common.radio.voice;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.AbstractRadioBlockEntity;
import com.arrl.radiocraft.common.radio.Radio;
import com.arrl.radiocraft.common.radio.RadioManager;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;

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

	public void onMicrophonePacket(MicrophonePacketEvent event) { // This should only be called serverside
		VoicechatServerApi voiceApi = event.getVoicechat();

		de.maxhenkel.voicechat.api.ServerPlayer sender = event.getSenderConnection().getPlayer();

		if(sender.getPlayer() instanceof ServerPlayer player) {
			double sqrRange = voiceApi.getBroadcastRange();
			sqrRange *= sqrRange;

			Map<BlockPos, Radio> radios = RadioManager.getNetwork(player.getLevel()).allRadios();

			for(BlockPos pos : radios.keySet()) { // All radios in range of the sender will receive the packet
				if(pos.distToCenterSqr(player.position()) < sqrRange) {
					BlockEntity blockEntity = player.getLevel().getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
					if(blockEntity instanceof AbstractRadioBlockEntity be) {
						be.acceptVoicePacket(voiceApi, sender.getServerLevel(), event.getPacket());
					}
				}
			}
		}
	}

}
