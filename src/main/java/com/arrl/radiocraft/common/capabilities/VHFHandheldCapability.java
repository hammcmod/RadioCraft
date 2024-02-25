package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.common.blockentities.AntennaBlockEntity;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.AntennaCWPacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetwork;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import com.arrl.radiocraft.common.radio.voice.RadiocraftVoicePlugin;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class VHFHandheldCapability implements IVHFHandheldCapability {

	private EntityAudioChannel receiveChannel = null;
	private WeakReference<Player> playerRef = null; // Use a weak ref here so the item isn't able to permanently load the entity.
	private AntennaNetwork network = null;
	private ItemStack heldItem = ItemStack.EMPTY;

	private boolean isPowered = false;
	private int frequency = 0;
	private boolean isPTTDown = false;
	private boolean isReceiving = true;

	@Override
	public ItemStack getItem() {
		return heldItem;
	}

	@Override
	public void setItem(ItemStack item) {
		heldItem = item;
	}

	@Override
	public int getFrequency() {
		return frequency;
	}

	@Override
	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	@Override
	public boolean isPowered() {
		return isPowered;
	}

	@Override
	public void setPowered(boolean value) {
		isPowered = value;
	}

	@Override
	public boolean isPTTDown() {
		return isPTTDown;
	}

	@Override
	public void setPTTDown(boolean value) {
		isPTTDown = value;
	}

	@Override
	public Player getPlayer() {
		return playerRef == null ? null : playerRef.get();
	}

	@Override
	public void setPlayer(Player player) {
		if(playerRef == null || playerRef.get() != player) {
			playerRef = new WeakReference<>(player);
			receiveChannel = null; // Every time the player changes, make sure to null the channel too.

			setNetwork(AntennaNetworkManager.getNetwork(player.getLevel(), AntennaNetworkManager.VHF_ID));
		}
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag nbt = new CompoundTag();
		nbt.put("inventory", heldItem.save(new CompoundTag()));
		nbt.putBoolean("isPowered", isPowered);
		nbt.putInt("frequency", frequency);
		nbt.putBoolean("isPTTDown", isPTTDown);
		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		heldItem = ItemStack.of(nbt.getCompound("inventory"));
		isPowered = nbt.getBoolean("isPowered");
		frequency = nbt.getInt("frequency");
		isPTTDown = nbt.getBoolean("isPTTDown");
	}

	@Override
	public boolean canTransmitVoice() {
		return isPowered && isPTTDown;
	}

	@Override
	public Vec3 getPos() {
		return playerRef == null ? null : playerRef.get().position();
	}

	@Override
	public BlockPos getBlockPos() {
		return playerRef == null ? null : playerRef.get().blockPosition();
	}

	@Override
	public void transmitCWPacket(net.minecraft.server.level.ServerLevel level, Collection<CWBuffer> buffers, int wavelength, int frequency) {
		// Handheld doesn't have CW capability.
	}

	@Override
	public void receiveCWPacket(AntennaCWPacket packet) {
		// Handheld doesn't have CW capability.
	}

	@Override
	public boolean isReceiving() {
		return isReceiving;
	}

	@Override
	public void setReceiving(boolean value) {
		isReceiving = value;
	}

	public void openChannel() {
		if(RadiocraftVoicePlugin.api == null)
			Radiocraft.LOGGER.error("Radiocraft VoiceChatServerApi is null.");
		receiveChannel = RadiocraftVoicePlugin.api.createEntityAudioChannel(UUID.randomUUID(), RadiocraftVoicePlugin.api.fromEntity(playerRef.get()));
	}

	@Override
	public void acceptVoicePacket(ServerLevel level, short[] rawAudio, UUID sourcePlayer) {
		transmitAudioPacket(level, rawAudio, 2, frequency, sourcePlayer);
	}

	@Override
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

	@Override
	public void transmitAudioPacket(ServerLevel level, short[] rawAudio, int wavelength, int frequency, UUID sourcePlayer) {
		if(network != null) {
			Set<IAntenna> antennas = network.allAntennas();

			for(IAntenna antenna : antennas) {
				if(antenna != this) {
					AntennaVoicePacket packet = new AntennaVoicePacket(level, rawAudio.clone(), wavelength, frequency, 1.0F, this, sourcePlayer);

					double distance = Math.sqrt(packet.getSource().getBlockPos().distSqr( antenna.getBlockPos()));
					packet.setStrength(BandUtils.getBaseStrength(packet.getWavelength(), distance, 1.0F, 0.0F, packet.getLevel().isDay()));

					antenna.receiveAudioPacket(packet);
				}
			}
		}
	}

	@Override
	public void receiveAudioPacket(AntennaVoicePacket packet) {
		// level#getBlockEntity is thread sensitive for some unknown reason.
		BlockPos pos = getBlockPos();
		if(network.getLevel().getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE) instanceof AntennaBlockEntity be)
			be.receiveAudioPacket(packet); // No change in strength as handheld antenna is tailored to be perfect.
	}

	public void setNetwork(AntennaNetwork network) {
		if(this.network != null)
			this.network.removeAntenna(this);
		network.addAntenna(this);
		this.network = network;
	}

}
