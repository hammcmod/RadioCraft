package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RepeaterNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.VHFRadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.VHFRepeaterMenu;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.voice.Ctcss;
import com.arrl.radiocraft.common.radio.voice.RepeaterAudio;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A 2m FM repeater: it listens on its input (receive) frequency and puts whatever it hears back out on its output
 * (transmit) frequency, as it arrives, so handhelds that can't reach each other can talk through it. Settings, all from
 * the front panel: the two frequencies, a CTCSS access tone (only signals carrying it are repeated) and an output tone,
 * a callsign it sends in Morse every so often, the hang time it holds the transmitter up after a user lets go (ending
 * in a courtesy beep) and a timeout timer that cuts off anyone talking too long.
 * <p>
 * To transmit and receive at once through one antenna it needs a duplexer on its back, top or bottom, with the
 * antenna's coax on the duplexer. With an antenna but no duplexer its own transmitter deafens its receiver: every
 * signal makes it key up, lose the signal, drop, and key up again, a string of short bursts of noise ("kerchunking").
 * With no antenna at all it works through the bare connector, over a few blocks.
 */
public class VHFRepeaterBlockEntity extends VHFRadioBlockEntity {

	/** Ticks without an input frame before the user counts as having let go. */
	public static final int INPUT_GAP_TICKS = 3;
	/** One kerchunk: noise this many ticks, then deaf for the rest of the cycle. */
	public static final int KERCHUNK_BURST_TICKS = 3;
	public static final int KERCHUNK_CYCLE_TICKS = 8;
	public static final int MAX_HANG_TICKS = 100;
	public static final int MAX_TIMEOUT_TICKS = 10 * 60 * 20;
	public static final int MAX_ID_INTERVAL_TICKS = 30 * 60 * 20;

	public static final float DEFAULT_OUTPUT = 146_940_000.0F;
	public static final float DEFAULT_INPUT = 146_340_000.0F;

	// Settings (saved, and synced to the screen).
	protected float txFrequency = DEFAULT_OUTPUT;
	protected float rxTone = Ctcss.NONE;
	protected float txTone = Ctcss.NONE;
	protected String callsign = "";
	protected int idIntervalTicks = 10 * 60 * 20;
	protected int hangTicks = 40;
	protected int timeoutTicks = 3 * 60 * 20;
	protected boolean txEnabled = true;
	protected boolean speakerEnabled = false;
	protected boolean micEnabled = false;

	// Running state. Input times are written from the voice thread.
	protected volatile long lastInputGameTime = -1L;
	protected volatile long inputStartGameTime = -1L;
	protected volatile long lastLocalGameTime = -1L; // The front panel mic.
	protected volatile boolean timedOut = false;
	protected volatile boolean hasDuplexer = false;
	protected volatile boolean hasAntenna = false;
	private boolean keyed = false;
	private boolean beepPending = false;
	private long hangUntil = -1L;
	private long lastIdGameTime = -1L;
	private int kerchunkTicks = 0;
	private final ConcurrentLinkedQueue<short[]> generated = new ConcurrentLinkedQueue<>();
	private final UUID voiceId;

	public VHFRepeaterBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.VHF_REPEATER.get(), pos, state, Band.getBand("2m"));
		frequency = DEFAULT_INPUT;
		voiceId = UUID.nameUUIDFromBytes(("radiocraft-repeater-" + pos.asLong()).getBytes(StandardCharsets.UTF_8));
	}

	// -------------------- SETTINGS --------------------

	public float getRxFrequency() {
		return frequency;
	}

	public float getTxFrequency() {
		return txFrequency;
	}

	public float getRxTone() {
		return rxTone;
	}

	public float getTxTone() {
		return txTone;
	}

	public String getCallsign() {
		return callsign;
	}

	public int getIdIntervalTicks() {
		return idIntervalTicks;
	}

	public int getHangTicks() {
		return hangTicks;
	}

	public int getTimeoutTicks() {
		return timeoutTicks;
	}

	public boolean isTxEnabled() {
		return txEnabled;
	}

	public boolean isSpeakerEnabled() {
		return speakerEnabled;
	}

	public boolean isMicEnabled() {
		return micEnabled;
	}

	/**
	 * Applies the settings present in {@code tag} (keys as in {@link #writeSettings}), each checked and clamped, and
	 * leaves the others alone. The screen sends only what it changed.
	 */
	public void applySettings(CompoundTag tag) {
		int step = getFrequencyStep();
		if(tag.contains("rxFrequency"))
			frequency = clampToBand(tag.getFloat("rxFrequency"), step);
		if(tag.contains("txFrequency"))
			txFrequency = clampToBand(tag.getFloat("txFrequency"), step);
		if(tag.contains("rxTone"))
			rxTone = Ctcss.sanitize(tag.getFloat("rxTone"));
		if(tag.contains("txTone"))
			txTone = Ctcss.sanitize(tag.getFloat("txTone"));
		if(tag.contains("callsign"))
			callsign = RepeaterAudio.sanitizeCallsign(tag.getString("callsign"));
		if(tag.contains("idInterval"))
			idIntervalTicks = Mth.clamp(tag.getInt("idInterval"), 0, MAX_ID_INTERVAL_TICKS);
		if(tag.contains("hang"))
			hangTicks = Mth.clamp(tag.getInt("hang"), 0, MAX_HANG_TICKS);
		if(tag.contains("timeout"))
			timeoutTicks = Mth.clamp(tag.getInt("timeout"), 0, MAX_TIMEOUT_TICKS);
		if(tag.contains("tx"))
			txEnabled = tag.getBoolean("tx");
		if(tag.contains("speaker"))
			speakerEnabled = tag.getBoolean("speaker");
		if(tag.contains("mic"))
			micEnabled = tag.getBoolean("mic");
		if(level != null && !level.isClientSide)
			updateIsReceiving();
		setChanged();
		updateBlock();
	}

	private float clampToBand(float hz, int step) {
		Band band = getBand();
		if(band == null)
			return hz;
		float clamped = Math.max(band.minFrequency(), Math.min(band.maxFrequency(), hz));
		return snapToStep(band, clamped, step, 0);
	}

	protected void writeSettings(CompoundTag nbt) {
		nbt.putFloat("txFrequency", txFrequency);
		nbt.putFloat("rxTone", rxTone);
		nbt.putFloat("txTone", txTone);
		nbt.putString("callsign", callsign);
		nbt.putInt("idInterval", idIntervalTicks);
		nbt.putInt("hang", hangTicks);
		nbt.putInt("timeout", timeoutTicks);
		nbt.putBoolean("tx", txEnabled);
		nbt.putBoolean("speaker", speakerEnabled);
		nbt.putBoolean("mic", micEnabled);
	}

	@Override
	protected void setupSaveTag(CompoundTag nbt) {
		super.setupSaveTag(nbt);
		writeSettings(nbt);
		nbt.putBoolean("hasDuplexer", hasDuplexer);
	}

	@Override
	protected void readSaveTag(CompoundTag nbt) {
		super.readSaveTag(nbt);
		if(nbt.contains("txFrequency")) {
			int step = getFrequencyStep();
			txFrequency = clampToBand(nbt.getFloat("txFrequency"), step);
			rxTone = Ctcss.sanitize(nbt.getFloat("rxTone"));
			txTone = Ctcss.sanitize(nbt.getFloat("txTone"));
			callsign = RepeaterAudio.sanitizeCallsign(nbt.getString("callsign"));
			idIntervalTicks = Mth.clamp(nbt.getInt("idInterval"), 0, MAX_ID_INTERVAL_TICKS);
			hangTicks = Mth.clamp(nbt.getInt("hang"), 0, MAX_HANG_TICKS);
			timeoutTicks = Mth.clamp(nbt.getInt("timeout"), 0, MAX_TIMEOUT_TICKS);
			txEnabled = nbt.getBoolean("tx");
			speakerEnabled = nbt.getBoolean("speaker");
			micEnabled = nbt.getBoolean("mic");
		}
		hasDuplexer = nbt.getBoolean("hasDuplexer");
	}

	// -------------------- RADIO HOOKS --------------------

	@Override
	public float getTransmitFrequency() {
		return txFrequency;
	}

	@Override
	public float getTransmitTone() {
		return txTone;
	}

	@Override
	public float getToneSquelch() {
		return rxTone;
	}

	/** The radio's offset and tone (from {@link VHFRadioBlockEntity}) are replaced by the two frequencies and tones. */
	@Override
	public void setOffsetHz(int offset) {}

	@Override
	public void setCtcssTone(float tone) {}

	private boolean isPowered() {
		return level != null && IBENetworks.getObject(level, worldPosition) instanceof RadioNetworkObject networkObject && networkObject.isPowered;
	}

	/** The repeater listens whenever it is on; the speaker switch only decides whether it plays what it hears. */
	@Override
	protected boolean isListening() {
		return isPowered();
	}

	@Override
	public void updateIsReceiving() {
		if(level != null && !level.isClientSide() && getNetworkObject(level, worldPosition) instanceof RadioNetworkObject networkObject) {
			networkObject.setTransmitting(keyed || kerchunkTicks > 0);
			getVoiceReceiver().setReceiving(networkObject.isPowered && speakerEnabled);
		}
	}

	/** The front panel mic picks up players near the repeater while MIC is on (and the transmitter is enabled). */
	@Override
	public boolean canTransmitVoice() {
		return micEnabled && txEnabled && isPowered();
	}

	@Override
	public void acceptVoicePacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, UUID sourcePlayer) {
		lastLocalGameTime = this.level == null ? 0L : this.level.getGameTime();
		super.acceptVoicePacket(level, rawAudio, sourcePlayer);
		recordTransmit();
	}

	/** No PTT to mute the receiver: the repeater hears while it transmits (that is what the duplexer is for). */
	@Override
	public boolean isReceiverMuted() {
		return false;
	}

	@Override
	public boolean shouldPlayStatic() {
		return false; // FM, squelched.
	}

	// -------------------- REPEATING --------------------

	/** A signal on the input with the right tone is repeated on the output as it arrives. Voice chat thread. */
	@Override
	public boolean receiveVoice(AntennaVoicePacket packet) {
		short[] clean = packet.getRawAudio().clone(); // The speaker's processing works on the packet's audio in place.
		boolean heard = super.receiveVoice(packet);
		if(heard)
			onInput(clean, packet);
		return heard;
	}

	private void onInput(short[] audio, AntennaVoicePacket packet) {
		long now = level == null ? 0L : level.getGameTime();
		if(lastInputGameTime < 0 || now - lastInputGameTime > INPUT_GAP_TICKS) { // A new over.
			inputStartGameTime = now;
			timedOut = false;
		}
		lastInputGameTime = now;
		if(!txEnabled)
			return;
		if(timeoutTicks > 0 && now - inputStartGameTime > timeoutTicks) {
			timedOut = true; // Talked too long: the transmitter drops until they let go.
			return;
		}
		if(isKerchunking())
			return; // The tick keys it up and down; the voice never gets through.
		ServerLevel serverLevel = packet.getLevel();
		transmitVoice(() -> serverLevel, audio, txFrequency, packet.getSourcePlayer(), txTone);
		recordTransmit();
	}

	/** True when the repeater has an antenna but no duplexer: its own transmitter deafens it. */
	public boolean isKerchunking() {
		return hasAntenna && !hasDuplexer;
	}

	public boolean hasDuplexer() {
		return hasDuplexer;
	}

	/** True while a signal is coming in on the input (with the right tone). */
	public boolean isInputActive() {
		return level != null && lastInputGameTime >= 0 && level.getGameTime() - lastInputGameTime <= INPUT_GAP_TICKS;
	}

	/** True while the repeater has its transmitter up (repeating, hanging, beeping, sending its ID, or kerchunking). */
	public boolean isTransmitting() {
		return level != null && lastTransmitGameTime >= 0 && level.getGameTime() - lastTransmitGameTime <= 2;
	}

	public boolean isTimedOut() {
		return timedOut && isInputActive();
	}

	@Override
	protected void additionalTick() {
		super.additionalTick();
		if(!(level instanceof ServerLevel serverLevel))
			return;
		long now = serverLevel.getGameTime();
		RadioNetworkObject networkObject = IBENetworks.getObject(level, worldPosition) instanceof RadioNetworkObject no ? no : null;
		boolean powered = networkObject != null && networkObject.isPowered;
		boolean duplexer = RepeaterNetworkObject.findDuplexerSide(level, worldPosition) != null;
		hasAntenna = networkObject != null && !networkObject.getAntennas().isEmpty();
		if(duplexer != hasDuplexer) {
			hasDuplexer = duplexer;
			setChanged();
			updateBlock();
		}

		boolean wasKeyed = keyed || kerchunkTicks > 0;
		if(!powered || !txEnabled) {
			keyed = false;
			kerchunkTicks = 0;
			generated.clear();
		}
		else if(isKerchunking())
			tickKerchunk(serverLevel, now);
		else
			tickRepeat(serverLevel, now);
		if(wasKeyed != (keyed || kerchunkTicks > 0))
			updateIsReceiving(); // Transmit draw while keyed.
	}

	private void tickRepeat(ServerLevel serverLevel, long now) {
		kerchunkTicks = 0;
		boolean input = isInputActive() && !timedOut;
		boolean local = lastLocalGameTime >= 0 && now - lastLocalGameTime <= INPUT_GAP_TICKS;
		if(input || local) { // The voice is going out as it arrives; just keep the hang timer topped up.
			keyed = true;
			beepPending = true;
			hangUntil = now + hangTicks;
			return;
		}
		if(!keyed)
			return;
		if(beepPending) {
			beepPending = false;
			generated.addAll(RepeaterAudio.courtesyBeep());
			if(isIdDue(now)) {
				generated.addAll(RepeaterAudio.morse(callsign));
				lastIdGameTime = now;
			}
		}
		int frames = now % 2 == 0 ? 3 : 2; // 2.5 frames of 20 ms per 50 ms tick.
		for(int i = 0; i < frames; i++) {
			short[] frame = generated.poll();
			if(frame == null) {
				if(now > hangUntil)
					break;
				frame = RepeaterAudio.silence(); // Carrier up, nobody talking.
			}
			transmitVoice(() -> serverLevel, frame, txFrequency, voiceId, txTone);
			recordTransmit();
		}
		if(generated.isEmpty() && now > hangUntil)
			keyed = false;
	}

	private boolean isIdDue(long now) {
		return !callsign.isEmpty() && idIntervalTicks > 0 && (lastIdGameTime < 0 || now - lastIdGameTime >= idIntervalTicks);
	}

	/**
	 * No duplexer: a signal keys the transmitter up, which deafens the receiver, so it drops, hears the signal again
	 * and keys up again. Out comes a burst of noise every few ticks for as long as someone is talking.
	 */
	private void tickKerchunk(ServerLevel serverLevel, long now) {
		keyed = false;
		generated.clear();
		if(kerchunkTicks == 0 && !isInputActive())
			return;
		int phase = kerchunkTicks % KERCHUNK_CYCLE_TICKS;
		kerchunkTicks++;
		if(phase < KERCHUNK_BURST_TICKS) {
			int frames = now % 2 == 0 ? 3 : 2;
			for(int i = 0; i < frames; i++) {
				transmitVoice(() -> serverLevel, RepeaterAudio.noise(0.35D, now * 7L + i), txFrequency, voiceId, txTone);
				recordTransmit();
			}
		}
		if(phase == KERCHUNK_CYCLE_TICKS - 1 && !isInputActive())
			kerchunkTicks = 0; // Nobody talking any more: it stays down.
	}

	/** Number of bursts sent so far in the current kerchunk run, for tests. */
	public int getKerchunkTicks() {
		return kerchunkTicks;
	}

	// -------------------- MENU --------------------

	public static final int DATA_DUPLEXER = 2;
	public static final int DATA_INPUT = 3;
	public static final int DATA_TRANSMIT = 4;
	public static final int DATA_ANTENNA = 5;
	public static final int REPEATER_DATA_COUNT = 6;

	@Override
	public ContainerData getDataSlots() {
		if(level == null || level.isClientSide)
			return new SimpleContainerData(REPEATER_DATA_COUNT);
		ContainerData radio = super.getDataSlots();
		return new ContainerData() {
			@Override
			public int get(int index) {
				return switch(index) {
					case DATA_DUPLEXER -> hasDuplexer ? 1 : 0;
					case DATA_INPUT -> isInputActive() ? (timedOut ? 2 : 1) : 0;
					case DATA_TRANSMIT -> isTransmitting() ? 1 : 0;
					case DATA_ANTENNA -> hasAntenna ? 1 : 0;
					default -> radio.get(index);
				};
			}

			@Override
			public void set(int index, int value) {}

			@Override
			public int getCount() {
				return REPEATER_DATA_COUNT;
			}
		};
	}

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("container.radiocraft.vhf_repeater");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
		return new VHFRepeaterMenu(id, this);
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new RepeaterNetworkObject(level, worldPosition, CommonConfig.VHF_REPEATER_TRANSMIT_TICK.get(), CommonConfig.VHF_REPEATER_RECEIVE_TICK.get());
	}
}
