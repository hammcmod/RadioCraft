package com.arrl.radiocraft.common.radio.voice;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.blockentities.radio.IVoiceTransmitter;
import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.api.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.radio.VoiceTransmitters;
import com.arrl.radiocraft.common.radio.voice.EncodingManager.EncodingData;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;

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

			// Decode voice packet
			EncodingData encodingData = encodingManager.getOrCreate(sender.getUuid());
			byte[] encodedAudio = event.getPacket().getOpusEncodedData();
			short[] decodedAudio = encodingData.getDecoder().decode(encodedAudio);
			if(encodedAudio.length == 0)
				encodingData.reset();
			else {
				LazyOptional<IVHFHandheldCapability> optional = getHandheldCapability(player);

				optional.ifPresent(cap -> {
					if(cap.getPlayer() != player) // If another player picked up the radio, or they changed dimension.
						cap.setPlayer(player);

					if(cap.canTransmitVoice())
						cap.acceptVoicePacket(sender.getServerLevel(), decodedAudio, sender.getUuid());
				});

				double sqrRange = api.getBroadcastRange();
				sqrRange *= sqrRange;

				List<IVoiceTransmitter> listeners = VoiceTransmitters.LISTENERS.get(player.getLevel());

				for (IVoiceTransmitter listener : listeners) { // All radios in range of the sender will receive the packet
					Vec3 pos = listener.getPos();

					if (pos.distanceToSqr(player.position()) > sqrRange)
						continue; // Do not transmit if out of range.

					if (!listener.canTransmitVoice())
						continue; // Do not transmit if listener is not accepting packets.

					listener.acceptVoicePacket(sender.getServerLevel(), decodedAudio, sender.getUuid());
				}
			}
		}
	}

	public boolean isTransmittingHandheld(ItemStack item) {
		if(item.getItem() == RadiocraftItems.VHF_HANDHELD.get()) {
			IVHFHandheldCapability cap = item.getCapability(RadiocraftCapabilities.VHF_HANDHELDS).orElse(null);
			if(cap != null) { // IntelliJ is lying, this can be false.
				return true;
			}
		}
		return false;
	}

	/**
	 * Attempt to find a valid VHF handheld on the given player.
	 * @param player The player to be checked.
	 * @return A {@link LazyOptional} containing the {@link IVHFHandheldCapability} of the found radio, or empty if none
	 * were found.
	 */
	public LazyOptional<IVHFHandheldCapability> getHandheldCapability(ServerPlayer player) {
		if(isTransmittingHandheld(player.getMainHandItem()))
			return player.getMainHandItem().getCapability(RadiocraftCapabilities.VHF_HANDHELDS);
		else if(isTransmittingHandheld(player.getOffhandItem()))
			return player.getOffhandItem().getCapability(RadiocraftCapabilities.VHF_HANDHELDS);

		return LazyOptional.empty();
	}

	public void onPlayerDisconnected(PlayerDisconnectedEvent event) {
		encodingManager.close(event.getPlayerUuid());
	}

}
