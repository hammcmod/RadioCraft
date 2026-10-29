package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.blocks.CommercialRadioModuleBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.CommercialStations;
import com.arrl.radiocraft.common.radio.antenna.CabinetAntenna;
import com.arrl.radiocraft.common.radio.voice.StationProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * A commercial radio module's works, in the bottom cell of its cabinet: a sealed all-band transmitter running one
 * {@link StationProgram} on one frequency for good, through its own antenna ({@link CabinetAntenna}). Players cannot
 * change it. A structure (or {@code /rcdebug module}) sets the program, band and frequency in its saved data; left
 * unset, it picks them at random the first time it runs. It is sent from {@link CommercialStations}, which keeps it on
 * the air while its chunk is unloaded too; the block registers it there and works the lamps.
 */
public class CommercialRadioModuleBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {

	public static final String ID = "radiocraft:commercial_radio_module";

	/** The bands it may pick from at random. */
	private static final String[] BANDS = { "80m", "40m", "20m", "10m", "2m" };

	private StationProgram.Kind kind = null;
	private String bandName = "";
	private float frequency;
	private long seed;
	private String callsign = "";

	/** Its station is on the list: false just loaded, or set up afresh. */
	private boolean registered = false;
	/** Its chunk is unloading: the station stays on the air. */
	private boolean unloading = false;

	public CommercialRadioModuleBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.COMMERCIAL_RADIO_MODULE.get(), pos, state);
	}

	/** Sets what it sends and where, as a structure or an operator's command does. */
	public void configure(StationProgram.Kind kind, Band band, float frequency) {
		this.kind = kind;
		this.bandName = band.name();
		this.frequency = frequency;
		registered = false;
		if(level instanceof ServerLevel server)
			register(server);
		setChanged();
		if(level != null && !level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Nullable
	public StationProgram.Kind getKind() {
		return kind;
	}

	public float getFrequency() {
		return frequency;
	}

	public String getBandName() {
		return bandName;
	}

	/**
	 * Writes a program, band, frequency, seed and callsign picked with {@code random} into a module's saved tag
	 * {@code tag}, for one at {@code pos} (the callsign's digit goes by where it is). A radio site's modules are set up
	 * like this as the site is placed, from a random source that the world seed and the site's spot fix.
	 */
	public static void preset(CompoundTag tag, Level level, BlockPos pos, RandomSource random) {
		StationProgram.Kind[] kinds = StationProgram.Kind.values();
		Band band = Band.getBand(BANDS[random.nextInt(BANDS.length)]);
		float step = band.isVhf() ? 5_000.0F : 1_000.0F;
		int channels = (int)((band.maxFrequency() - band.minFrequency()) / step);
		float hz = band.minFrequency() + step * random.nextInt(Math.max(1, channels));
		tag.putLong("seed", random.nextLong());
		if(tag.getString("callsign").isEmpty())
			tag.putString("callsign", com.arrl.radiocraft.common.radio.Callsigns.generateRepeater(level, pos, random));
		tag.putString("program", kinds[random.nextInt(kinds.length)].name().toLowerCase(Locale.ROOT));
		tag.putString("band", band.name());
		tag.putFloat("frequency", hz);
	}

	/** Picks a program, band and frequency at random. */
	private void randomize(ServerLevel level) {
		CompoundTag tag = new CompoundTag();
		tag.putString("callsign", callsign);
		preset(tag, level, worldPosition, level.getRandom());
		seed = tag.getLong("seed");
		callsign = tag.getString("callsign");
		configure(StationProgram.Kind.byName(tag.getString("program")), Band.getBand(tag.getString("band")), tag.getFloat("frequency"));
	}

	/** This module's station, or null before it has a program. */
	@Nullable
	public CommercialStations.Station station() {
		if(kind == null || Band.getBand(bandName) == null)
			return null;
		return new CommercialStations.Station(worldPosition.immutable(), kind, bandName, frequency, seed, callsign, false);
	}

	private void register(ServerLevel level) {
		CommercialStations.Station station = station();
		if(station == null)
			return;
		CommercialStations.get(level).register(station);
		registered = true;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, CommercialRadioModuleBlockEntity module) {
		if(level instanceof ServerLevel serverLevel)
			module.serverTick(serverLevel);
	}

	private void serverTick(ServerLevel level) {
		if(kind == null || Band.getBand(bandName) == null)
			randomize(level);
		if(!registered || level.getGameTime() % 20 == 0)
			register(level); // Again now and then: a no-op unless the list lost it.
		lamps(level, CommercialStations.get(level).isSending(worldPosition));
	}

	/**
	 * A recording from the server's numbers tapes for a numbers station to read out instead of Morse, cut into frames;
	 * null if there are none (or the one picked is still being decoded: Morse this time, the tape next).
	 */
	@Nullable
	public static List<short[]> numbersTape(ServerLevel level) {
		List<String> tracks = com.arrl.radiocraft.common.radio.tape.TapeLibrary.tracks(level.getServer(), com.arrl.radiocraft.common.radio.tape.TapeLibrary.NUMBERS);
		if(tracks.isEmpty())
			return null;
		short[] audio = com.arrl.radiocraft.common.radio.tape.TapeLibrary.audio(level.getServer(), tracks.get(level.getRandom().nextInt(tracks.size())));
		if(audio == null || audio.length == 0)
			return null;
		int frame = com.arrl.radiocraft.common.radio.voice.RepeaterAudio.FRAME;
		List<short[]> frames = new java.util.ArrayList<>();
		for(int from = 0; from < audio.length; from += frame)
			frames.add(java.util.Arrays.copyOfRange(audio, from, from + frame)); // The last one padded with silence.
		return frames;
	}

	/** The control panel's lamps, in cell 2: power always, transmit while it sends. */
	private void lamps(ServerLevel level, boolean transmitting) {
		BlockPos panel = worldPosition.above(2);
		BlockState state = level.getBlockState(panel);
		if(!(state.getBlock() instanceof CommercialRadioModuleBlock))
			return;
		BlockState lit = state.setValue(CommercialRadioModuleBlock.LAMP_PWR, true).setValue(CommercialRadioModuleBlock.LAMP_TX, transmitting);
		if(lit != state)
			level.setBlock(panel, lit, Block.UPDATE_CLIENTS);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.network.chat.Component getDisplayName() {
		return net.minecraft.network.chat.Component.translatable("block.radiocraft.commercial_radio_module");
	}

	@Override
	public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player player) {
		return new com.arrl.radiocraft.common.menus.CommercialRadioModuleMenu(id, this);
	}

	/** Broken (not unloaded): its station goes off the air. */
	@Override
	public void setRemoved() {
		super.setRemoved();
		if(!unloading && level instanceof ServerLevel server)
			CommercialStations.get(server).remove(worldPosition);
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		unloading = true;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		if(kind != null)
			tag.putString("program", kind.name().toLowerCase(Locale.ROOT));
		tag.putString("band", bandName);
		tag.putFloat("frequency", frequency);
		tag.putLong("seed", seed);
		tag.putString("callsign", callsign);
	}

	/** Also how a structure presets one: {@code program} (numbers, static_beacon, dead_air, df_beeper, cw_beacon), {@code band}, {@code frequency} in Hz. */
	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		kind = tag.contains("program") ? StationProgram.Kind.byName(tag.getString("program")) : null;
		bandName = tag.getString("band");
		frequency = tag.getFloat("frequency");
		seed = tag.contains("seed") ? tag.getLong("seed") : worldPosition.asLong();
		callsign = tag.getString("callsign");
		registered = false;
	}
}
