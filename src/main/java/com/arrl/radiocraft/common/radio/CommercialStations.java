package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blockentities.CommercialRadioModuleBlockEntity;
import com.arrl.radiocraft.common.blocks.StackedBlocks;
import com.arrl.radiocraft.common.radio.antenna.CabinetAntenna;
import com.arrl.radiocraft.common.radio.voice.StationProgram;
import com.arrl.radiocraft.common.worldgen.SitePrediction;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Every commercial radio module station in a level, on the air whether its chunk is loaded or not: a module's program
 * only needs the game time, and its antenna only a position, so the station is sent from here, every tick, and the
 * module block only works its lamps. A module registers itself while it is loaded and is struck off when it is broken
 * (or found missing once its chunk loads).
 * <p>
 * Radio sites nobody has been near yet are on the air too: around each player, every spot where the world generator
 * would put a radio site is worked out the way it would ({@link SitePrediction}), and a site that would have a module
 * goes on the list as a predicted station, with the very program, frequency and callsign the module will have when the
 * site is generated. Each spot is only looked at once ({@link #checked}), off the server thread, a few at a time,
 * nearest first.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public class CommercialStations extends SavedData {

	private static final String DATA_NAME = Radiocraft.MOD_ID + "_commercial_stations";
	/** Grid cells being worked out at once (each costs the noise of a few columns, a few milliseconds, on the world generator's threads). */
	private static final int IN_FLIGHT = 1;
	/** How far a station a real module registers clears predicted ones round it (a site predicted a block off). */
	private static final int CLEARS = 48;

	/**
	 * One station: where the module's bottom cell is, its program, band (by name), frequency in Hz, seed and callsign,
	 * and whether it is only predicted (its site not generated yet).
	 */
	public record Station(BlockPos pos, StationProgram.Kind kind, String band, float frequency, long seed, String callsign, boolean predicted) {

		/** A module's station from its saved tag ({@link CommercialRadioModuleBlockEntity}'s keys); null if it has no program yet. */
		@Nullable
		public static Station fromModuleTag(BlockPos pos, CompoundTag tag, boolean predicted) {
			if(!tag.contains("program") || Band.getBand(tag.getString("band")) == null)
				return null;
			return new Station(pos.immutable(), StationProgram.Kind.byName(tag.getString("program")), tag.getString("band"), tag.getFloat("frequency"),
					tag.getLong("seed"), tag.getString("callsign"), predicted);
		}

		private CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putLong("pos", pos.asLong());
			tag.putString("program", kind.name().toLowerCase(Locale.ROOT));
			tag.putString("band", band);
			tag.putFloat("frequency", frequency);
			tag.putLong("seed", seed);
			tag.putString("callsign", callsign);
			tag.putBoolean("predicted", predicted);
			return tag;
		}

		private static Station load(CompoundTag tag) {
			return new Station(BlockPos.of(tag.getLong("pos")), StationProgram.Kind.byName(tag.getString("program")), tag.getString("band"),
					tag.getFloat("frequency"), tag.getLong("seed"), tag.getString("callsign"), tag.getBoolean("predicted"));
		}
	}

	/** A station on the air: its program's place in its schedule and its antenna, made when it first sends. */
	private record Live(Station station, StationProgram program, CabinetAntenna antenna, UUID voiceId) {}

	private final Map<BlockPos, Station> stations = new LinkedHashMap<>();
	/** The cells of each radio site grid already looked at (by structure set), as {@link ChunkPos#asLong} of the cell. */
	private final Map<String, LongOpenHashSet> checked = new HashMap<>();

	private final Map<BlockPos, Live> live = new HashMap<>();
	private final Set<BlockPos> sending = new HashSet<>();
	/** Cells to look at, by structure set, nearest a player first. */
	private final Map<String, LongLinkedOpenHashSet> queue = new LinkedHashMap<>();
	/** What the cells being worked out turned up, one list a cell (empty for none), to put on the air on the server thread. */
	private final ConcurrentLinkedQueue<List<Station>> found = new ConcurrentLinkedQueue<>();
	private int inFlight = 0;

	public static CommercialStations get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(new Factory<>(CommercialStations::new, CommercialStations::load), DATA_NAME);
	}

	private static CommercialStations load(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		CommercialStations data = new CommercialStations();
		ListTag list = tag.getList("stations", Tag.TAG_COMPOUND);
		for(int i = 0; i < list.size(); i++) {
			Station station = Station.load(list.getCompound(i));
			data.stations.put(station.pos(), station);
		}
		CompoundTag checked = tag.getCompound("checked");
		for(String set : checked.getAllKeys())
			data.checked(set).addAll(LongArrayList.wrap(checked.getLongArray(set)));
		return data;
	}

	@Override
	public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		ListTag list = new ListTag();
		for(Station station : stations.values())
			list.add(station.save());
		tag.put("stations", list);
		CompoundTag cells = new CompoundTag();
		checked.forEach((set, done) -> cells.put(set, new LongArrayTag(done.toLongArray())));
		tag.put("checked", cells);
		return tag;
	}

	// -------------------- THE LIST --------------------

	/** The stations, a snapshot. */
	public List<Station> stations() {
		return List.copyOf(stations.values());
	}

	@Nullable
	public Station get(BlockPos pos) {
		return stations.get(pos);
	}

	/** True if the station at {@code pos} sent something on the last tick (a module's TX lamp). */
	public boolean isSending(BlockPos pos) {
		return sending.contains(pos);
	}

	/**
	 * Puts {@code station} on the air, or changes it. A real module's station also clears the predicted ones round it,
	 * in case its site was predicted a little off.
	 */
	public void register(Station station) {
		if(station.equals(stations.get(station.pos())))
			return;
		if(!station.predicted())
			for(Station other : List.copyOf(stations.values()))
				if(other.predicted() && !other.pos().equals(station.pos()) && Math.abs(other.pos().getX() - station.pos().getX()) <= CLEARS
						&& Math.abs(other.pos().getZ() - station.pos().getZ()) <= CLEARS)
					remove(other.pos());
		stations.put(station.pos(), station);
		drop(station.pos());
		setDirty();
	}

	/** Takes the station at {@code pos} off the air (its module was broken). */
	public void remove(BlockPos pos) {
		if(stations.remove(pos) != null)
			setDirty();
		drop(pos);
		sending.remove(pos);
	}

	private void drop(BlockPos pos) {
		Live gone = live.remove(pos);
		if(gone != null)
			gone.antenna().unregister();
	}

	// -------------------- ON THE AIR --------------------

	private void tick(ServerLevel level) {
		long now = level.getGameTime();
		sending.clear();
		for(Station station : stations.values()) {
			Band band = Band.getBand(station.band());
			if(band == null)
				continue;
			Live on = live.computeIfAbsent(station.pos(), pos -> start(level, station, band));
			List<short[]> frames = on.program().frames(now, now % 2 == 0 ? 3 : 2);
			if(frames.isEmpty())
				continue;
			on.antenna().transmitAudioFrames(() -> level, frames, band, station.frequency(), on.voiceId());
			sending.add(station.pos());
		}
		if(now % 20 == 0)
			strikeOffMissing(level);
		if(now % 100 == 0)
			queueCells(level);
		predict(level);
	}

	private static Live start(ServerLevel level, Station station, Band band) {
		CabinetAntenna antenna = new CabinetAntenna(level, station.pos().above(StackedBlocks.HEIGHT - 1));
		antenna.register(band);
		StationProgram program = new StationProgram(station.kind(), station.seed(), station.callsign().isEmpty() ? "X0X" : station.callsign(),
				() -> CommercialRadioModuleBlockEntity.numbersTape(level));
		UUID voiceId = UUID.nameUUIDFromBytes(("radiocraft-module-" + station.pos().asLong()).getBytes(StandardCharsets.UTF_8));
		return new Live(station, program, antenna, voiceId);
	}

	/** Strikes off the stations whose chunk is loaded with no module there: broken while unloaded, or predicted wrong. */
	private void strikeOffMissing(ServerLevel level) {
		for(Station station : List.copyOf(stations.values()))
			if(level.isLoaded(station.pos()) && !(level.getBlockEntity(station.pos()) instanceof CommercialRadioModuleBlockEntity))
				remove(station.pos());
	}

	// -------------------- SITES NOT GENERATED YET --------------------

	private LongOpenHashSet checked(String set) {
		return checked.computeIfAbsent(set, key -> new LongOpenHashSet());
	}

	/** Queues the cells of each site grid within {@code commercial_station_range} of a player not looked at yet, nearest first. */
	private void queueCells(ServerLevel level) {
		int range = RadiocraftServerConfig.COMMERCIAL_STATION_RANGE.get();
		if(range <= 0)
			return;
		for(SitePrediction prediction : SitePrediction.of(level)) {
			String set = prediction.key().location().toString();
			LongOpenHashSet done = checked(set);
			LongLinkedOpenHashSet todo = queue.computeIfAbsent(set, key -> new LongLinkedOpenHashSet());
			int cellBlocks = prediction.spacing() * 16, reach = range / cellBlocks + 1;
			for(ServerPlayer player : level.players()) {
				int cx = Math.floorDiv(player.getBlockX(), cellBlocks), cz = Math.floorDiv(player.getBlockZ(), cellBlocks);
				List<long[]> near = new ArrayList<>();
				for(int dx = -reach; dx <= reach; dx++)
					for(int dz = -reach; dz <= reach; dz++)
						if((long)dx * dx + (long)dz * dz <= (long)reach * reach && !done.contains(ChunkPos.asLong(cx + dx, cz + dz)))
							near.add(new long[] { (long)dx * dx + (long)dz * dz, ChunkPos.asLong(cx + dx, cz + dz) });
				near.sort(Comparator.comparingLong(cell -> cell[0]));
				for(long[] cell : near)
					todo.add(cell[1]);
			}
		}
	}

	/**
	 * Puts on the air what the cells worked out so far turned up, and sets more cells going (off the server thread,
	 * as the world generator checks its structures) while fewer than {@link #IN_FLIGHT} are.
	 */
	private void predict(ServerLevel level) {
		for(List<Station> cell; (cell = found.poll()) != null; ) {
			inFlight--;
			for(Station station : cell)
				if(!stations.containsKey(station.pos()))
					register(station);
		}
		for(SitePrediction prediction : SitePrediction.of(level)) {
			LongLinkedOpenHashSet todo = queue.get(prediction.key().location().toString());
			LongOpenHashSet done = checked(prediction.key().location().toString());
			while(todo != null && !todo.isEmpty() && inFlight < IN_FLIGHT) {
				long cell = todo.removeFirstLong();
				if(!done.add(cell))
					continue;
				setDirty();
				inFlight++;
				CompletableFuture.supplyAsync(() -> prediction.stations(ChunkPos.getX(cell), ChunkPos.getZ(cell)), Util.backgroundExecutor())
						.whenComplete((cellStations, error) -> {
							if(error != null)
								Radiocraft.LOGGER.error("Working out the radio site in cell {} of {}", ChunkPos.getX(cell) + ", " + ChunkPos.getZ(cell),
										prediction.key().location(), error);
							found.add(cellStations == null ? List.of() : cellStations);
						});
			}
		}
	}

	// -------------------- EVENTS --------------------

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if(event.getLevel() instanceof ServerLevel level)
			get(level).tick(level);
	}

	/** The antennas leave the (global) antenna networks with their level. */
	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if(event.getLevel() instanceof ServerLevel level) {
			CommercialStations data = level.getDataStorage().get(new Factory<>(CommercialStations::new, CommercialStations::load), DATA_NAME);
			if(data != null) {
				for(Live on : data.live.values())
					on.antenna().unregister();
				data.live.clear();
			}
		}
	}
}
