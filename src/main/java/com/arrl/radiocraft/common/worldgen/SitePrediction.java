package com.arrl.radiocraft.common.worldgen;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.CommercialStations;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Where the world generator will put radio sites, worked out ahead of it, as {@code /locate} does: for one cell of
 * a radio site placement grid ({@link #SETS}: the sites anywhere, and the mountain-top ones on a closer grid since
 * they are found on so little of the ground), the chunk it picks and which site it settles on there (the same
 * weighted draw, each site's own biome, slope and height checks, from the same noise), then that site's commercial
 * radio module stations ({@link SitePiece#stations}). Nothing is generated or loaded. Thread safe once made, as the
 * world generator's own structure checks are: {@link #stations} may run off the server thread.
 */
public final class SitePrediction {

	public static final ResourceKey<StructureSet> RADIO_SITES = ResourceKey.create(Registries.STRUCTURE_SET, Radiocraft.id("radio_sites"));
	public static final ResourceKey<StructureSet> RADIO_MOUNTAIN_SITES = ResourceKey.create(Registries.STRUCTURE_SET, Radiocraft.id("radio_mountain_sites"));
	/** The structure sets of radio sites. */
	public static final List<ResourceKey<StructureSet>> SETS = List.of(RADIO_SITES, RADIO_MOUNTAIN_SITES);

	private static final Map<ServerLevel, List<SitePrediction>> FOR_LEVEL = new WeakHashMap<>();

	private final ServerLevel level;
	private final ResourceKey<StructureSet> key;
	private final StructureSet set;
	private final RandomSpreadStructurePlacement placement;

	private SitePrediction(ServerLevel level, ResourceKey<StructureSet> key, StructureSet set, RandomSpreadStructurePlacement placement) {
		this.level = level;
		this.key = key;
		this.set = set;
		this.placement = placement;
	}

	/** The predictions for {@code level}, one a set of sites its generator places: none in a flat world or the Nether. */
	public static List<SitePrediction> of(ServerLevel level) {
		return FOR_LEVEL.computeIfAbsent(level, SitePrediction::make);
	}

	/** The prediction for set {@code key} in {@code level}, or null if its generator does not place it. */
	@Nullable
	public static SitePrediction of(ServerLevel level, ResourceKey<StructureSet> key) {
		for(SitePrediction prediction : of(level))
			if(prediction.key().equals(key))
				return prediction;
		return null;
	}

	private static List<SitePrediction> make(ServerLevel level) {
		List<SitePrediction> out = new ArrayList<>();
		for(ResourceKey<StructureSet> key : SETS) {
			Optional<Holder.Reference<StructureSet>> set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).getHolder(key);
			if(set.isEmpty() || !(set.get().value().placement() instanceof RandomSpreadStructurePlacement spread))
				continue;
			if(level.getChunkSource().getGeneratorState().possibleStructureSets().stream().anyMatch(holder -> holder.is(key)))
				out.add(new SitePrediction(level, key, set.get().value(), spread));
		}
		return List.copyOf(out);
	}

	public ResourceKey<StructureSet> key() {
		return key;
	}

	/** The placement grid's spacing, in chunks. */
	public int spacing() {
		return placement.spacing();
	}

	/** The site the generator will put in grid cell {@code cellX}, {@code cellZ}, if any. */
	public Optional<StructureStart> site(int cellX, int cellZ) {
		ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
		long seed = state.getLevelSeed();
		ChunkPos chunk = placement.getPotentialStructureChunk(seed, cellX * placement.spacing(), cellZ * placement.spacing());
		if(!placement.isStructureChunk(state, chunk.x, chunk.z))
			return Optional.empty();
		// As ChunkGenerator#createStructures: one site alone is tried; several are drawn by weight, each that fails
		// dropped and the draw made again.
		List<StructureSet.StructureSelectionEntry> entries = new ArrayList<>(set.structures());
		if(entries.size() == 1)
			return tryGenerate(entries.getFirst(), chunk, state);
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
		random.setLargeFeatureSeed(seed, chunk.x, chunk.z);
		int total = 0;
		for(StructureSet.StructureSelectionEntry entry : entries)
			total += entry.weight();
		while(!entries.isEmpty()) {
			int draw = random.nextInt(total), index = 0;
			for(StructureSet.StructureSelectionEntry entry : entries) {
				draw -= entry.weight();
				if(draw < 0)
					break;
				index++;
			}
			StructureSet.StructureSelectionEntry entry = entries.get(index);
			Optional<StructureStart> start = tryGenerate(entry, chunk, state);
			if(start.isPresent())
				return start;
			entries.remove(index);
			total -= entry.weight();
		}
		return Optional.empty();
	}

	private Optional<StructureStart> tryGenerate(StructureSet.StructureSelectionEntry entry, ChunkPos chunk, ChunkGeneratorStructureState state) {
		Structure structure = entry.structure().value();
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		StructureStart start = structure.generate(level.registryAccess(), generator, generator.getBiomeSource(), state.randomState(), level.getStructureManager(),
				state.getLevelSeed(), chunk, 0, level, structure.biomes()::contains);
		return start.isValid() ? Optional.of(start) : Optional.empty();
	}

	/** The commercial radio module stations of the site in grid cell {@code cellX}, {@code cellZ}. */
	public List<CommercialStations.Station> stations(int cellX, int cellZ) {
		List<CommercialStations.Station> out = new ArrayList<>();
		site(cellX, cellZ).ifPresent(start -> {
			for(StructurePiece piece : start.getPieces())
				if(piece instanceof SitePiece site)
					out.addAll(site.stations(level));
		});
		return out;
	}
}
