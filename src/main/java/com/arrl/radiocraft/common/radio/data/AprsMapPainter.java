package com.arrl.radiocraft.common.radio.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Paints an APRS map (a vanilla map an H19 made, see {@code AprsTerminalBlockEntity#createMap}): greyscale terrain
 * from the loaded chunks, kept dark so the markers stand out, near black where nothing is loaded, a lighter grid, a
 * target at the home station and a white banner, labelled with its callsign, at each station heard. It is centred on
 * them all and scaled to fit, in powers of two so it does not jitter as stations come and go.
 * <p>
 * The map's own centre and scale are fixed when it is made (0, 0 at 1:1), so the painter works in map pixels and
 * places markers by pixel, not by world position.
 */
public final class AprsMapPainter {

	public static final int SIZE = 128;
	/** Fewest blocks a map shows across, so a lone station is not drawn a pixel per block. */
	public static final int MIN_SPAN = 128;
	private static final String MARKER_PREFIX = "aprs-";

	/** Grey map colours from black to white, as packed map colour ids with their brightness 0 to 1. */
	private static final List<double[]> GREYS = new ArrayList<>();

	static {
		MapColor[] bases = { MapColor.COLOR_BLACK, MapColor.COLOR_GRAY, MapColor.DEEPSLATE, MapColor.STONE, MapColor.COLOR_LIGHT_GRAY, MapColor.WOOL, MapColor.SNOW };
		for(MapColor base : bases) {
			for(MapColor.Brightness brightness : MapColor.Brightness.values()) {
				int rgb = base.col;
				double luminance = (0.299D * ((rgb >> 16) & 0xFF) + 0.587D * ((rgb >> 8) & 0xFF) + 0.114D * (rgb & 0xFF)) / 255.0D * brightness.modifier / 255.0D;
				GREYS.add(new double[] { luminance, base.getPackedId(brightness) });
			}
		}
	}

	private AprsMapPainter() {}

	/** Where the map looks: the block at its middle and how many blocks one pixel covers (a power of two). */
	public record Fit(int centerX, int centerZ, int blocksPerPixel) {

		/** The map pixel (0 to 127, may fall outside) a world column lands on. */
		public int pixelX(double x) {
			return (int)Math.floor((x - centerX) / blocksPerPixel) + SIZE / 2;
		}

		public int pixelZ(double z) {
			return (int)Math.floor((z - centerZ) / blocksPerPixel) + SIZE / 2;
		}
	}

	/** Centred on {@code home} and {@code stations} together, with a margin, at least {@link #MIN_SPAN} blocks across. */
	public static Fit fit(BlockPos home, List<BlockPos> stations) {
		int minX = home.getX(), maxX = minX, minZ = home.getZ(), maxZ = minZ;
		for(BlockPos at : stations) {
			minX = Math.min(minX, at.getX());
			maxX = Math.max(maxX, at.getX());
			minZ = Math.min(minZ, at.getZ());
			maxZ = Math.max(maxZ, at.getZ());
		}
		double span = Math.max(MIN_SPAN, Math.max(maxX - minX, maxZ - minZ) * 1.25D + 16);
		int blocksPerPixel = 1;
		while(blocksPerPixel * SIZE < span && blocksPerPixel < (1 << 16))
			blocksPerPixel *= 2;
		int step = blocksPerPixel * 8; // The centre moves in whole grid steps, so the picture does not crawl.
		int centerX = Math.floorDiv((minX + maxX) / 2 + step / 2, step) * step;
		int centerZ = Math.floorDiv((minZ + maxZ) / 2 + step / 2, step) * step;
		return new Fit(centerX, centerZ, blocksPerPixel);
	}

	/** The picture for {@code fit}: one packed map colour per pixel, row by row (x fastest), as map data holds them. */
	public static byte[] paint(ServerLevel level, Fit fit) {
		byte[] colors = new byte[SIZE * SIZE];
		int grid = gridStep(fit.blocksPerPixel());
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for(int px = 0; px < SIZE; px++) {
			int x = fit.centerX() + (px - SIZE / 2) * fit.blocksPerPixel() + fit.blocksPerPixel() / 2;
			int northHeight = Integer.MIN_VALUE;
			for(int pz = -1; pz < SIZE; pz++) { // Row -1 only gives the first row a northern neighbour to shade against.
				int z = fit.centerZ() + (pz - SIZE / 2) * fit.blocksPerPixel() + fit.blocksPerPixel() / 2;
				double luminance;
				int height = Integer.MIN_VALUE;
				if(!level.hasChunk(x >> 4, z >> 4))
					luminance = 0.06D; // Not loaded: unknown ground.
				else {
					height = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
					pos.set(x, height, z);
					BlockState state = level.getBlockState(pos);
					MapColor color = state.getMapColor(level, pos);
					int rgb = color == MapColor.NONE ? 0 : color.col;
					double base = (0.299D * ((rgb >> 16) & 0xFF) + 0.587D * ((rgb >> 8) & 0xFF) + 0.114D * (rgb & 0xFF)) / 255.0D;
					double shade = northHeight == Integer.MIN_VALUE ? 1.0D : height > northHeight ? 1.15D : height < northHeight ? 0.8D : 1.0D;
					luminance = 0.12D + 0.4D * Mth.clamp(base * shade, 0.0D, 1.0D); // Kept dark: the markers are white.
				}
				northHeight = height;
				if(pz < 0)
					continue;
				boolean onGrid = Math.floorMod(x - fit.blocksPerPixel() / 2, grid) < fit.blocksPerPixel()
						|| Math.floorMod(z - fit.blocksPerPixel() / 2, grid) < fit.blocksPerPixel();
				if(onGrid)
					luminance = Math.max(luminance, 0.62D);
				colors[px + pz * SIZE] = grey(luminance);
			}
		}
		return colors;
	}

	/** Grid lines about every 32 pixels, at a round number of blocks. */
	public static int gridStep(int blocksPerPixel) {
		int step = 16;
		while(step < blocksPerPixel * 32)
			step *= 2;
		return step;
	}

	/** The packed map colour of the grey nearest {@code luminance}. */
	public static byte grey(double luminance) {
		double[] best = GREYS.getFirst();
		for(double[] grey : GREYS)
			if(Math.abs(grey[0] - luminance) < Math.abs(best[0] - luminance))
				best = grey;
		return (byte)best[1];
	}

	/** A station to mark: its callsign and where it is. */
	public record Station(String callsign, BlockPos pos) {}

	/** Puts {@code colors} and the markers on {@code data}; only what changed goes out to players. */
	public static void apply(ServerLevel level, MapItemSavedData data, Fit fit, byte[] colors, BlockPos home, List<Station> stations) {
		for(int px = 0; px < SIZE; px++)
			for(int pz = 0; pz < SIZE; pz++)
				data.updateColor(px, pz, colors[px + pz * SIZE]);
		Set<String> keep = new HashSet<>();
		marker(level, data, fit, MARKER_PREFIX + "home", home, true, null, keep);
		for(Station station : stations)
			marker(level, data, fit, MARKER_PREFIX + station.callsign(), station.pos(), false, Component.literal(station.callsign()), keep);
		Set<String> placed = MarkerIds.of(data); // Stations no longer heard, or off the map now, come off it.
		for(String id : List.copyOf(placed)) {
			if(!keep.contains(id)) {
				data.removeDecoration(id);
				placed.remove(id);
			}
		}
	}

	private static void marker(ServerLevel level, MapItemSavedData data, Fit fit, String id, BlockPos at, boolean home, Component name, Set<String> keep) {
		int px = fit.pixelX(at.getX() + 0.5D), pz = fit.pixelZ(at.getZ() + 0.5D);
		if(px < 0 || px >= SIZE || pz < 0 || pz >= SIZE)
			return;
		// The map was made centred on 0, 0 at one block a pixel: a "world" position of the pixel offset lands on it.
		data.addDecoration(home ? MapDecorationTypes.TARGET_POINT : MapDecorationTypes.WHITE_BANNER, level, id,
				px - SIZE / 2 + 0.5D, pz - SIZE / 2 + 0.5D, 180.0D, name);
		keep.add(id);
		MarkerIds.of(data).add(id);
	}

	/** The marker ids each map has had, so ones for stations no longer heard can be taken off. */
	private static final class MarkerIds {
		private static final java.util.Map<MapItemSavedData, Set<String>> IDS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

		static Set<String> of(MapItemSavedData data) {
			return IDS.computeIfAbsent(data, ignored -> new HashSet<>());
		}
	}
}
