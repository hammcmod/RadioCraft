package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.api.blockentities.radio.IVoiceTransmitter;
import com.arrl.radiocraft.common.be_networks.network_objects.MicrowaveNetworkObjects;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.TelephoneMenu;
import com.arrl.radiocraft.common.radio.VoiceTransmitters;
import com.arrl.radiocraft.common.radio.voice.RadioVoiceProcessor;
import com.arrl.radiocraft.common.radio.voice.RadiocraftVoicePlugin;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A desk telephone on a microwave radio (touching it, or on its power wire). Each phone has an extension, numbered
 * from 0 as phones are placed; its owner can change it to any number no phone it can reach already has. Dial an
 * extension: a phone on the same radio, or on any radio across the microwave link, rings until it is answered (or
 * for 30 seconds). While a call is up, voices near either phone come out of the other at phone quality
 * ({@link #PHONE_QUALITY}): fuller than SSB, and clean, as the link is digital.
 */
public class TelephoneBlockEntity extends BlockEntity implements INetworkObjectProvider, MenuProvider, IVoiceTransmitter {

	/** A landline's voice band (300-3400 Hz), lightly compressed, no noise: fuller than SSB's 300-2700. */
	public static final RadioVoiceProcessor.Settings PHONE_QUALITY = new RadioVoiceProcessor.Settings(300.0D, 3400.0D, 1.2D, true);
	/** Ticks a phone rings before the caller hears no answer. */
	public static final int RING_TICKS = 600;
	/** Blocks from the phone a voice is still picked up, and how far its earpiece is heard. */
	public static final double PICKUP = 4.0D;
	public static final float EARPIECE = 8.0F;

	public enum State { IDLE, CALLING, RINGING, IN_CALL }

	private static final Map<ResourceKey<Level>, Map<BlockPos, TelephoneBlockEntity>> LOADED = new ConcurrentHashMap<>();

	private int extension = -1;
	private State state = State.IDLE;
	@Nullable
	private BlockPos partner = null;
	private int partnerExtension = -1;
	private String status = "";
	private int stateTicks = 0;

	// The other phone while in a call, for the voice thread.
	@Nullable
	private volatile TelephoneBlockEntity partnerPhone = null;
	private volatile boolean lossy = false; // Interference somewhere on the link.
	private LocationalAudioChannel earpiece = null; // Voice thread only.
	private final RadioVoiceProcessor processor = new RadioVoiceProcessor();
	private OpusEncoder encoder = null;

	public TelephoneBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.TELEPHONE.get(), pos, state);
	}

	public static Collection<TelephoneBlockEntity> loaded(Level level) {
		Map<BlockPos, TelephoneBlockEntity> map = LOADED.get(level.dimension());
		return map == null ? List.of() : List.copyOf(map.values());
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(level != null && !level.isClientSide) {
			LOADED.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()).put(worldPosition, this);
			VoiceTransmitters.addListener(level, this);
			getNetworkObject(level, worldPosition);
			if(extension < 0 && level.getServer() != null) {
				extension = TelephoneExtensions.get(level.getServer()).next();
				setChanged();
			}
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if(level != null && !level.isClientSide) {
			Map<BlockPos, TelephoneBlockEntity> map = LOADED.get(level.dimension());
			if(map != null)
				map.remove(worldPosition, this);
			VoiceTransmitters.removeListener(level, this);
			if(state != State.IDLE)
				hangUp();
			synchronized(this) {
				if(encoder != null)
					encoder.close();
				encoder = null;
				earpiece = null;
			}
		}
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		setRemoved();
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new MicrowaveNetworkObjects.StationLink(level, worldPosition);
	}

	// -------------------- REACH --------------------

	/** Phones this one can call: on the same microwave radio, or on a radio linked to one of its radios. */
	public List<TelephoneBlockEntity> reachable() {
		List<TelephoneBlockEntity> out = new ArrayList<>();
		if(level == null)
			return out;
		List<MicrowaveRadioBlockEntity> radios = new ArrayList<>();
		for(MicrowaveRadioBlockEntity radio : MicrowaveRadioBlockEntity.servingStation(level, worldPosition)) {
			if(!radio.isPowered())
				continue;
			if(!radios.contains(radio))
				radios.add(radio);
			for(MicrowaveRadioBlockEntity peer : radio.peers())
				if(!radios.contains(peer))
					radios.add(peer);
		}
		for(MicrowaveRadioBlockEntity radio : radios)
			for(BlockEntity station : radio.stations())
				if(station instanceof TelephoneBlockEntity phone && phone != this && !out.contains(phone))
					out.add(phone);
		return out;
	}

	/** True if any radio on the way to {@code other} has interference, or a signal too weak for a clean call. */
	private boolean lossyTo(TelephoneBlockEntity other) {
		if(level == null)
			return false;
		for(MicrowaveRadioBlockEntity radio : MicrowaveRadioBlockEntity.servingStation(level, worldPosition))
			if(radio.hasInterference() || radio.isMarginal())
				return true;
		for(MicrowaveRadioBlockEntity radio : MicrowaveRadioBlockEntity.servingStation(level, other.worldPosition))
			if(radio.hasInterference() || radio.isMarginal())
				return true;
		return false;
	}

	// -------------------- CALLS --------------------

	public static void tick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if(!level.isClientSide && blockEntity instanceof TelephoneBlockEntity phone)
			phone.serverTick();
	}

	private void serverTick() {
		if(level == null || state == State.IDLE)
			return;
		stateTicks++;
		TelephoneBlockEntity other = partner == null ? null : at(level, partner);
		if(other == null || other.partner == null || !other.partner.equals(worldPosition)) {
			drop("CALL DROPPED");
			return;
		}
		if(state == State.RINGING && stateTicks % 40 == 1) // The bell: two rings every two seconds.
			level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
		if(state == State.RINGING && stateTicks % 40 == 6)
			level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
		if(state == State.CALLING && stateTicks >= RING_TICKS) {
			other.reset("MISSED CALL FROM " + extension);
			reset("NO ANSWER");
			return;
		}
		if(stateTicks % 20 == 0) { // Still reachable?
			if(!reachable().contains(other)) {
				other.drop("CALL DROPPED");
				drop("CALL DROPPED");
				return;
			}
			lossy = lossyTo(other);
		}
	}

	@Nullable
	private static TelephoneBlockEntity at(Level level, BlockPos pos) {
		Map<BlockPos, TelephoneBlockEntity> map = LOADED.get(level.dimension());
		TelephoneBlockEntity phone = map == null ? null : map.get(pos);
		return phone == null || phone.isRemoved() ? null : phone;
	}

	/** Dials {@code number}: rings that phone if it can be reached and is free. */
	public void dial(int number) {
		if(level == null || state != State.IDLE)
			return;
		if(number == extension) {
			status = "THAT IS THIS PHONE";
			sync();
			return;
		}
		if(MicrowaveRadioBlockEntity.servingStation(level, worldPosition).stream().noneMatch(MicrowaveRadioBlockEntity::isPowered)) {
			status = "NO LINE: NO MICROWAVE RADIO ON";
			sync();
			return;
		}
		TelephoneBlockEntity callee = reachable().stream().filter(phone -> phone.extension == number).findFirst().orElse(null);
		if(callee == null)
			status = "EXTENSION " + number + " NOT FOUND";
		else if(callee.state != State.IDLE)
			status = "BUSY";
		else {
			state = State.CALLING;
			partner = callee.worldPosition;
			partnerExtension = number;
			stateTicks = 0;
			status = "";
			callee.state = State.RINGING;
			callee.partner = worldPosition;
			callee.partnerExtension = extension;
			callee.stateTicks = 0;
			callee.status = "";
			callee.sync();
		}
		sync();
	}

	/** Picks up a ringing phone: the call is up. */
	public void answer() {
		if(level == null || state != State.RINGING || partner == null)
			return;
		TelephoneBlockEntity caller = at(level, partner);
		if(caller == null) {
			drop("CALL DROPPED");
			return;
		}
		state = caller.state = State.IN_CALL;
		stateTicks = caller.stateTicks = 0;
		partnerPhone = caller;
		caller.partnerPhone = this;
		lossy = caller.lossy = lossyTo(caller);
		caller.sync();
		sync();
		level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.6F, 1.4F);
	}

	/** Hangs up, ending the call at both ends. */
	public void hangUp() {
		if(level != null && partner != null) {
			TelephoneBlockEntity other = at(level, partner);
			if(other != null && worldPosition.equals(other.partner))
				other.reset(state == State.RINGING ? "CALL REJECTED" : "CALL ENDED");
		}
		reset("");
	}

	private void drop(String why) {
		reset(why);
	}

	private void reset(String why) {
		state = State.IDLE;
		partner = null;
		partnerPhone = null;
		partnerExtension = -1;
		stateTicks = 0;
		status = why;
		sync();
	}

	/** Changes the extension; false if a phone it can reach has it already. */
	public boolean setExtension(int number) {
		if(number < 0 || number > 99999)
			return false;
		if(reachable().stream().anyMatch(phone -> phone.extension == number))
			return false;
		extension = number;
		sync();
		return true;
	}

	private void sync() {
		setChanged();
		if(level != null && !level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	// -------------------- VOICE --------------------

	@Override
	public boolean canTransmitVoice() {
		return state == State.IN_CALL && partnerPhone != null;
	}

	@Override
	public Vec3 getPos() {
		return worldPosition.getCenter();
	}

	@Override
	public void acceptVoicePacket(ServerLevel level, short[] rawAudio, UUID sourcePlayer) {
		acceptVoicePacket(level, rawAudio, sourcePlayer, null);
	}

	/** A voice near this phone in a call: out of the other phone, quieter the further the talker stands from this one. */
	@Override
	public void acceptVoicePacket(ServerLevel level, short[] rawAudio, UUID sourcePlayer, @Nullable Vec3 speakerPos) {
		TelephoneBlockEntity other = partnerPhone;
		if(other == null || state != State.IN_CALL)
			return;
		double gain = 1.0D;
		if(speakerPos != null) {
			double distance = speakerPos.distanceTo(getPos());
			if(distance > PICKUP)
				return;
			gain = RadioBlockEntity.deskMicGain(distance);
		}
		if(lossy && java.util.concurrent.ThreadLocalRandom.current().nextFloat() < 0.3F)
			return; // A frame lost to interference on the link.
		other.playIncoming(level, rawAudio.clone(), gain);
	}

	/** Plays a frame from the other end out of this phone's earpiece. Voice thread. */
	private synchronized void playIncoming(ServerLevel voiceLevel, short[] pcm, double gain) {
		if(RadiocraftVoicePlugin.API == null)
			return;
		if(earpiece == null) {
			earpiece = RadiocraftVoicePlugin.API.createLocationalAudioChannel(UUID.randomUUID(), voiceLevel,
					RadiocraftVoicePlugin.API.createPosition(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D));
			if(earpiece == null)
				return;
			earpiece.setDistance(EARPIECE);
			encoder = RadiocraftVoicePlugin.API.createEncoder();
		}
		earpiece.send(encoder.encode(processor.process(pcm, PHONE_QUALITY, Math.min(1.0D, gain), 0.0D)));
	}

	// -------------------- READING, SAVE, MENU --------------------

	public int getExtension() {
		return Math.max(0, extension);
	}

	public State getState() {
		return state;
	}

	public int getPartnerExtension() {
		return partnerExtension;
	}

	public String getStatus() {
		return status;
	}

	public int getStateTicks() {
		return stateTicks;
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt("extension", extension);
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		extension = tag.contains("extension") ? tag.getInt("extension") : -1;
		if(tag.contains("state")) { // The client's copy.
			try {
				state = State.valueOf(tag.getString("state"));
			}
			catch(IllegalArgumentException e) {
				state = State.IDLE;
			}
			partnerExtension = tag.getInt("partnerExtension");
			status = tag.getString("status");
		}
	}

	@Override
	public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
		CompoundTag tag = saveWithoutMetadata(registries);
		tag.putString("state", state.name());
		tag.putInt("partnerExtension", partnerExtension);
		tag.putString("status", status);
		return tag;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable(Radiocraft.translationKey("container", "telephone"));
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
		return new TelephoneMenu(id, this);
	}

	/** Extensions handed out in order to new phones, from 0, for the whole server. */
	public static class TelephoneExtensions extends net.minecraft.world.level.saveddata.SavedData {
		private int next = 0;

		public static TelephoneExtensions get(net.minecraft.server.MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(TelephoneExtensions::new, (tag, registries) -> {
				TelephoneExtensions out = new TelephoneExtensions();
				out.next = tag.getInt("next");
				return out;
			}, null), "radiocraft_telephone_extensions");
		}

		public int next() {
			setDirty();
			return next++;
		}

		@Override
		public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
			tag.putInt("next", next);
			return tag;
		}
	}
}
