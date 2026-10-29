package com.arrl.radiocraft.common.worldgen;

import com.arrl.radiocraft.common.init.RadiocraftStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A radio site found in the world: one structure template ({@code data/radiocraft/structure/sites}) set down as it
 * was built, never turned or mirrored, since cable bundles and the horns' and dishes' bearings are kept in their block
 * entities, which a turned template does not turn. It goes at the lowest of its corners and middle, {@code y_offset} below
 * the template's ground layer, in one of its biomes on dry ground no steeper than {@code max_slope} blocks across the footprint and no lower
 * than {@code min_y} (a mountain top, for a site that wants the height). {@link SitePiece} places it with the
 * {@link SiteProcessor}, which gives every copy its own optional parts ({@code variants}) and callsigns.
 */
public class SiteStructure extends Structure {

	public static final MapCodec<SiteStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			settingsCodec(instance),
			ResourceLocation.CODEC.fieldOf("template").forGetter(site -> site.template),
			Codec.INT.optionalFieldOf("ground", 1).forGetter(site -> site.ground),
			Codec.INT.optionalFieldOf("y_offset", 0).forGetter(site -> site.yOffset),
			Codec.INT.optionalFieldOf("max_slope", 6).forGetter(site -> site.maxSlope),
			Codec.INT.optionalFieldOf("min_y", Integer.MIN_VALUE).forGetter(site -> site.minY),
			SiteVariant.CODEC.listOf().optionalFieldOf("variants", List.of()).forGetter(site -> site.variants)
	).apply(instance, SiteStructure::new));

	private final ResourceLocation template;
	/** Layers of the template below its ground level: foundations, set into the ground. */
	private final int ground;
	private final int yOffset;
	private final int maxSlope;
	/** The lowest the ground under the site may be. */
	private final int minY;
	private final List<SiteVariant> variants;

	public SiteStructure(StructureSettings settings, ResourceLocation template, int ground, int yOffset, int maxSlope, int minY, List<SiteVariant> variants) {
		super(settings);
		this.template = template;
		this.ground = ground;
		this.yOffset = yOffset;
		this.maxSlope = maxSlope;
		this.minY = minY;
		this.variants = List.copyOf(variants);
	}

	/** The site's optional parts. */
	public List<SiteVariant> variants() {
		return variants;
	}

	/**
	 * Cheapest test first, as each height costs the noise of a whole column: the middle of the footprint (high
	 * enough, dry, in the right biome there), then the corners one at a time, stopping as soon as the slope is too
	 * steep. The site goes at the lowest of them.
	 */
	@Override
	protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext context) {
		Vec3i size = context.structureTemplateManager().getOrCreate(template).getSize();
		if(size.getX() == 0)
			return Optional.empty(); // No such template.
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getMinBlockX(), z = chunk.getMinBlockZ();
		int midX = x + size.getX() / 2, midZ = z + size.getZ() / 2;
		Heights mid = heights(context, midX, midZ);
		int middle = mid.surface();
		if(middle < minY || middle > mid.floor())
			return Optional.empty(); // Too low, or water on top: a lake, a river, the sea.
		if(!context.validBiome().test(context.chunkGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(midX), QuartPos.fromBlock(middle),
				QuartPos.fromBlock(midZ), context.randomState().sampler())))
			return Optional.empty(); // Not a biome it is found in (checked again where it goes, as for every structure).
		int low = middle, high = middle;
		for(int[] corner : new int[][] { { 0, 0 }, { size.getX() - 1, 0 }, { 0, size.getZ() - 1 }, { size.getX() - 1, size.getZ() - 1 } }) {
			int height = heights(context, x + corner[0], z + corner[1]).surface();
			low = Math.min(low, height);
			high = Math.max(high, height);
			if(low < minY || high - low > maxSlope)
				return Optional.empty();
		}
		BlockPos origin = new BlockPos(x, low - ground + yOffset, z);
		return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new SitePiece(context.structureTemplateManager(), template, origin, variants))));
	}

	/** The top block of a column ({@link Heightmap.Types#WORLD_SURFACE_WG}) and the top one that is not water ({@code OCEAN_FLOOR_WG}). */
	private record Heights(int surface, int floor) {}

	private record Column(RandomState noise, int x, int z) {}

	/** The columns worked out lately on this thread: the sites tried in one chunk share some, and a column's noise is dear. */
	private static final ThreadLocal<Map<Column, Heights>> COLUMNS = ThreadLocal.withInitial(HashMap::new);

	private static Heights heights(GenerationContext context, int x, int z) {
		Map<Column, Heights> columns = COLUMNS.get();
		Column key = new Column(context.randomState(), x, z);
		Heights known = columns.get(key);
		if(known != null)
			return known;
		LevelHeightAccessor height = context.heightAccessor();
		NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, height, context.randomState());
		int surface = Integer.MIN_VALUE, floor = Integer.MIN_VALUE;
		for(int y = height.getMaxBuildHeight() - 1; y >= height.getMinBuildHeight() && (surface == Integer.MIN_VALUE || floor == Integer.MIN_VALUE); y--) {
			BlockState state = column.getBlock(y);
			if(surface == Integer.MIN_VALUE && Heightmap.Types.WORLD_SURFACE_WG.isOpaque().test(state))
				surface = y;
			if(floor == Integer.MIN_VALUE && Heightmap.Types.OCEAN_FLOOR_WG.isOpaque().test(state))
				floor = y;
		}
		Heights heights = new Heights(surface == Integer.MIN_VALUE ? height.getMinBuildHeight() - 1 : surface,
				floor == Integer.MIN_VALUE ? height.getMinBuildHeight() - 1 : floor);
		if(columns.size() >= 256)
			columns.clear();
		columns.put(key, heights);
		return heights;
	}

	@Override
	public @NotNull StructureType<?> type() {
		return RadiocraftStructures.SITE.get();
	}
}
