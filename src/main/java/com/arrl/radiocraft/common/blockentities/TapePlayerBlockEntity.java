package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.blockentities.radio.IVoiceTransmitter;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.blocks.TapePlayerBlock;
import com.arrl.radiocraft.common.datacomponents.TapeContents;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.init.RadiocraftDataComponent;
import com.arrl.radiocraft.common.items.TapeItem;
import com.arrl.radiocraft.common.menus.TapePlayerMenu;
import com.arrl.radiocraft.common.radio.VoiceTransmitters;
import com.arrl.radiocraft.common.radio.tape.TapeLibrary;
import com.arrl.radiocraft.common.radio.tape.TapeRecordings;
import com.arrl.radiocraft.common.radio.voice.RadioVoiceProcessor;
import com.arrl.radiocraft.common.radio.voice.RadiocraftVoicePlugin;
import com.arrl.radiocraft.common.radio.voice.RepeaterAudio;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * A reel-to-reel tape player. It plays the tape in it out loud and into any radio touching it (keying the radio, as a
 * microphone would): once on PLAY, over and over, every so many minutes, or on a redstone pulse. On a blank or
 * recorded tape it records the voices of players near it, up to {@link TapeRecordings#MAX_SECONDS}.
 */
public class TapePlayerBlockEntity extends BlockEntity implements MenuProvider, IVoiceTransmitter {

	public enum Mode { MANUAL, LOOP, INTERVAL, REDSTONE }

	public static final int STATE_IDLE = 0, STATE_PLAYING = 1, STATE_RECORDING = 2, STATE_LOADING = 3;
	/** How far it hears a voice to record, blocks. */
	public static final double RECORD_RANGE = 6.0D;
	public static final int MAX_INTERVAL_MINUTES = 60;
	private static final int FRAME = RepeaterAudio.FRAME;
	private static final long FRAME_NANOS = 20_000_000L;

	private ItemStack tape = ItemStack.EMPTY;
	private Mode mode = Mode.MANUAL;
	private int intervalMinutes = 5;
	private int state = STATE_IDLE;
	private int position; // Frames played.
	private long nextPlay = -1L;
	private boolean wasPowered;

	// Recording, filled on the voice chat thread: each player's frames land in the 20 ms slot they were spoken in, mixed.
	private final short[] recordBuffer = new short[TapeRecordings.MAX_SECONDS * (int)RadioVoiceProcessor.SAMPLE_RATE];
	private volatile boolean recording;
	private long recordStart;
	private int recordedFrames;

	@Nullable
	private LocationalAudioChannel channel;
	@Nullable
	private OpusEncoder encoder;
	private final List<RadioBlockEntity> keyed = new ArrayList<>();
	private final UUID voiceId;

	public TapePlayerBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.TAPE_PLAYER.get(), pos, state);
		voiceId = UUID.nameUUIDFromBytes(("radiocraft-tape-" + pos.asLong()).getBytes(StandardCharsets.UTF_8));
	}

	// -------------------- THE TAPE --------------------

	public ItemStack getTape() {
		return tape;
	}

	/** Puts {@code stack} in, if the player is empty. True if it went in. */
	public boolean insert(ItemStack stack) {
		if(!tape.isEmpty() || !(stack.getItem() instanceof TapeItem))
			return false;
		tape = stack.copyWithCount(1);
		position = 0;
		nextPlay = -1L;
		changed();
		return true;
	}

	/** Takes the tape out (stopping it), or empty if there is none. */
	public ItemStack eject() {
		stop();
		ItemStack out = tape;
		tape = ItemStack.EMPTY;
		changed();
		return out;
	}

	private TapeContents contents() {
		return TapeItem.contents(tape);
	}

	/** The tape's audio: null while it loads, empty if there is nothing on it. */
	@Nullable
	private short[] audio(ServerLevel level) {
		TapeContents contents = contents();
		if(tape.isEmpty() || contents.isBlank())
			return new short[0];
		if(contents.isFound())
			return TapeLibrary.audio(level.getServer(), contents.track());
		UUID id = contents.recordingId();
		return id == null ? new short[0] : TapeRecordings.audio(level.getServer(), id);
	}

	// -------------------- CONTROLS --------------------

	public void play() {
		if(tape.isEmpty() || recording || contents().isBlank())
			return;
		state = STATE_LOADING;
		position = 0;
		changed();
	}

	public void stop() {
		if(recording)
			finishRecording();
		state = STATE_IDLE;
		position = 0;
		releaseRadios();
		changed();
	}

	/** Records over the tape (a found tape cannot be recorded on). */
	public void record() {
		if(tape.isEmpty() || contents().isFound() || level == null)
			return;
		stop();
		synchronized(recordBuffer) {
			java.util.Arrays.fill(recordBuffer, (short)0);
			recordedFrames = 0;
			recordStart = System.nanoTime();
		}
		recording = true;
		state = STATE_RECORDING;
		changed();
	}

	public void setMode(Mode value) {
		mode = value;
		nextPlay = -1L;
		changed();
	}

	public void setIntervalMinutes(int minutes) {
		intervalMinutes = Math.max(1, Math.min(MAX_INTERVAL_MINUTES, minutes));
		nextPlay = -1L;
		changed();
	}

	public Mode getMode() {
		return mode;
	}

	public int getIntervalMinutes() {
		return intervalMinutes;
	}

	// -------------------- TICK --------------------

	public static void tick(Level level, BlockPos pos, BlockState blockState, TapePlayerBlockEntity player) {
		if(level instanceof ServerLevel serverLevel)
			player.serverTick(serverLevel);
	}

	// Client side: the reels, this tick and the last, in degrees, and how fast they turn.
	private float reels, reelsBefore, reelSpeed;

	/** Client side, each tick: the reels spin up while it runs and coast to a stop when it does not. */
	public static void clientTick(Level level, BlockPos pos, BlockState blockState, TapePlayerBlockEntity player) {
		player.reelsBefore = player.reels;
		float target = blockState.getValue(TapePlayerBlock.RUNNING) ? 9.0F : 0.0F;
		player.reelSpeed += (target - player.reelSpeed) * 0.15F;
		player.reels = (player.reels + player.reelSpeed) % 360.0F;
		if(player.reels < player.reelsBefore)
			player.reelsBefore -= 360.0F;
	}

	/** How far round the reels are, degrees, between the last tick and this one. */
	public float reelAngle(float partialTick) {
		return net.minecraft.util.Mth.lerp(partialTick, reelsBefore, reels);
	}

	private void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		boolean powered = level.hasNeighborSignal(worldPosition);
		boolean pulse = powered && !wasPowered;
		wasPowered = powered;
		if(state == STATE_IDLE && !tape.isEmpty() && !contents().isBlank()) {
			boolean start = switch(mode) {
				case MANUAL -> false;
				case LOOP -> nextPlay < 0 || now >= nextPlay;
				case INTERVAL -> nextPlay < 0 || now >= nextPlay;
				case REDSTONE -> pulse;
			};
			if(start) {
				if(mode == Mode.INTERVAL)
					nextPlay = now + intervalMinutes * 1200L;
				play();
			}
		}
		if(state == STATE_RECORDING) {
			if(recordedSlots() >= TapeRecordings.MAX_SECONDS * 50)
				stop();
			return;
		}
		if(state != STATE_LOADING && state != STATE_PLAYING)
			return;
		short[] audio = audio(level);
		if(audio == null)
			return; // Still loading.
		if(state == STATE_LOADING) {
			state = STATE_PLAYING;
			keyRadios(level);
			changed();
		}
		int count = now % 2 == 0 ? 3 : 2;
		for(int i = 0; i < count; i++) {
			int from = position * FRAME;
			if(from >= audio.length) {
				finished(now);
				return;
			}
			short[] frame = new short[FRAME];
			System.arraycopy(audio, from, frame, 0, Math.min(FRAME, audio.length - from));
			position++;
			send(level, frame);
		}
	}

	private void finished(long now) {
		state = STATE_IDLE;
		position = 0;
		releaseRadios();
		if(mode == Mode.LOOP)
			nextPlay = now + 20; // A second's pause, then round again.
		changed();
	}

	/** One frame out loud, and into every radio touching it. */
	private void send(ServerLevel level, short[] frame) {
		if(RadiocraftVoicePlugin.API == null)
			return;
		if(channel == null) {
			channel = RadiocraftVoicePlugin.API.createLocationalAudioChannel(UUID.randomUUID(), RadiocraftVoicePlugin.API.fromServerLevel(level),
					RadiocraftVoicePlugin.API.createPosition(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D));
			if(channel != null)
				channel.setDistance(16.0F);
			encoder = RadiocraftVoicePlugin.API.createEncoder();
		}
		if(channel != null && encoder != null)
			channel.send(encoder.encode(frame.clone()));
		de.maxhenkel.voicechat.api.ServerLevel voiceLevel = RadiocraftVoicePlugin.API.fromServerLevel(level);
		for(RadioBlockEntity radio : keyed)
			if(!radio.isRemoved() && radio.canTransmitVoice())
				radio.acceptVoicePacket(voiceLevel, frame.clone(), voiceId);
	}

	/** Keys every radio touching the player, as its push-to-talk would. */
	private void keyRadios(ServerLevel level) {
		releaseRadios();
		for(Direction side : Direction.values())
			if(level.getBlockEntity(worldPosition.relative(side)) instanceof RadioBlockEntity radio) {
				radio.setPTTDown(true);
				keyed.add(radio);
			}
	}

	private void releaseRadios() {
		for(RadioBlockEntity radio : keyed)
			if(!radio.isRemoved())
				radio.setPTTDown(false);
		keyed.clear();
	}

	// -------------------- RECORDING --------------------

	private int recordedSlots() {
		synchronized(recordBuffer) {
			return recordedFrames;
		}
	}

	@Override
	public boolean canTransmitVoice() {
		return recording;
	}

	@Override
	public Vec3 getPos() {
		return worldPosition.getCenter();
	}

	@Override
	public void acceptVoicePacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, UUID sourcePlayer) {
		if(!recording)
			return;
		synchronized(recordBuffer) {
			int slot = (int)((System.nanoTime() - recordStart) / FRAME_NANOS);
			int from = slot * FRAME;
			if(from + FRAME > recordBuffer.length)
				return;
			for(int i = 0; i < FRAME && i < rawAudio.length; i++)
				recordBuffer[from + i] = (short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, recordBuffer[from + i] + rawAudio[i]));
			recordedFrames = Math.max(recordedFrames, slot + 1);
		}
	}

	@Override
	public void acceptVoicePacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, UUID sourcePlayer, Vec3 speakerPos) {
		if(speakerPos == null || speakerPos.distanceTo(getPos()) <= RECORD_RANGE)
			acceptVoicePacket(level, rawAudio, sourcePlayer);
	}

	/** Saves what was recorded onto the tape (nothing changes if nobody spoke). */
	private void finishRecording() {
		recording = false;
		short[] audio;
		synchronized(recordBuffer) {
			audio = java.util.Arrays.copyOf(recordBuffer, recordedFrames * FRAME);
		}
		if(audio.length == 0 || !(level instanceof ServerLevel serverLevel) || tape.isEmpty())
			return;
		UUID id = contents().recordingId() != null ? contents().recordingId() : UUID.randomUUID();
		TapeRecordings.save(serverLevel.getServer(), id, audio);
		tape.set(RadiocraftDataComponent.TAPE_CONTENTS.get(), contents().withRecording(id, Math.round(audio.length / (float)RadioVoiceProcessor.SAMPLE_RATE)));
	}

	// -------------------- STATUS --------------------

	public int getState() {
		return state;
	}

	/** Seconds into the tape (or recorded so far). */
	public int getSeconds() {
		return state == STATE_RECORDING ? recordedSlots() / 50 : position / 50;
	}

	/** 0 no tape, 1 blank, 2 a found tape, 3 recorded. */
	public int getTapeKind() {
		if(tape.isEmpty())
			return 0;
		TapeContents contents = contents();
		return contents.isFound() ? 2 : contents.isRecorded() ? 3 : 1;
	}

	public static final int DATA_MODE = 0, DATA_INTERVAL = 1, DATA_STATE = 2, DATA_SECONDS = 3, DATA_TAPE = 4, DATA_COUNT = 5;

	public ContainerData dataSlots() {
		return new ContainerData() {
			@Override
			public int get(int index) {
				return switch(index) {
					case DATA_MODE -> mode.ordinal();
					case DATA_INTERVAL -> intervalMinutes;
					case DATA_STATE -> state;
					case DATA_SECONDS -> getSeconds();
					case DATA_TAPE -> getTapeKind();
					default -> 0;
				};
			}

			@Override
			public void set(int index, int value) {}

			@Override
			public int getCount() {
				return DATA_COUNT;
			}
		};
	}

	/** Marks the change, tells clients (the tape's label), and turns the reels on the block while it plays or records. */
	private void changed() {
		setChanged();
		if(level != null && !level.isClientSide) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
			BlockState blockState = getBlockState();
			boolean running = state == STATE_PLAYING || state == STATE_RECORDING;
			if(blockState.hasProperty(TapePlayerBlock.RUNNING) && blockState.getValue(TapePlayerBlock.RUNNING) != running)
				level.setBlock(worldPosition, blockState.setValue(TapePlayerBlock.RUNNING, running), Block.UPDATE_CLIENTS);
		}
	}

	// -------------------- LIFE --------------------

	@Override
	public void onLoad() {
		super.onLoad();
		if(level != null && !level.isClientSide)
			VoiceTransmitters.addListener(level, this);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if(level != null && !level.isClientSide) {
			VoiceTransmitters.removeListener(level, this);
			releaseRadios();
		}
		if(encoder != null) {
			encoder.close();
			encoder = null;
		}
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		setRemoved();
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.saveAdditional(tag, registries);
		if(!tape.isEmpty())
			tag.put("tape", tape.save(registries));
		tag.putString("mode", mode.name().toLowerCase(Locale.ROOT));
		tag.putInt("interval", intervalMinutes);
	}

	/** Also how a structure presets one: {@code tape} (an item), {@code mode} (manual, loop, interval, redstone), {@code interval} in minutes. */
	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		tape = tag.contains("tape") ? ItemStack.parseOptional(registries, tag.getCompound("tape")) : ItemStack.EMPTY;
		try {
			mode = Mode.valueOf(tag.getString("mode").toUpperCase(Locale.ROOT));
		}
		catch(IllegalArgumentException e) {
			mode = Mode.MANUAL;
		}
		intervalMinutes = tag.contains("interval") ? Math.max(1, tag.getInt("interval")) : 5;
	}

	@Override
	public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("container.radiocraft.tape_player");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
		return new TapePlayerMenu(id, this, dataSlots());
	}
}
