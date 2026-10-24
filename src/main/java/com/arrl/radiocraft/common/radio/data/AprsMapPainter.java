package com.arrl.radiocraft.common.radio.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Paints an APRS map (a vanilla map an H19 made, see {@code AprsTerminalBlockEntity#createMap}) in the style of an old
 * terminal: black land with contour lines, dithered water with a bright shoreline, a dotted grid (see {@link #paint}),
 * a target at the home station and a white banner, labelled with its callsign, at each station heard. It is centred on
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

	/** Height between contour lines, in blocks. */
	public static final int CONTOUR = 8;
	// The few tones of the DOS-style map, as luminance 0 (black) to 1 (white).
	private static final double LAND = 0.0D, CONTOUR_LINE = 0.42D, WATER_DOT = 0.22D, SHORE = 0.8D, GRID_DOT = 0.3D;

	/**
	 * The picture for {@code fit}, drawn like an old terminal's map rather than a shaded one: black land with grey
	 * contour lines every {@link #CONTOUR} blocks of height, water as a dark dither edged by a bright shoreline, a
	 * dotted grid, and black where nothing is loaded. One packed map colour per pixel, row by row (x fastest).
	 */
	public static byte[] paint(ServerLevel level, Fit fit) {
		int[] height = new int[(SIZE + 1) * (SIZE + 1)]; // With a row and a column of neighbours to the north and west.
		byte[] kind = new byte[(SIZE + 1) * (SIZE + 1)]; // 0 unknown, 1 land, 2 water.
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for(int px = -1; px < SIZE; px++) {
			for(int pz = -1; pz < SIZE; pz++) {
				int x = blockAt(fit.centerX(), px, fit.blocksPerPixel()), z = blockAt(fit.centerZ(), pz, fit.blocksPerPixel());
				int i = (px + 1) + (pz + 1) * (SIZE + 1);
				if(!level.hasChunk(x >> 4, z >> 4))
					continue;
				int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
				pos.set(x, top, z);
				height[i] = top;
				kind[i] = (byte)(level.getFluidState(pos).is(FluidTags.WATER) ? 2 : 1);
			}
		}
		byte[] colors = new byte[SIZE * SIZE];
		int grid = gridStep(fit.blocksPerPixel());
		for(int px = 0; px < SIZE; px++) {
			for(int pz = 0; pz < SIZE; pz++) {
				int i = (px + 1) + (pz + 1) * (SIZE + 1), west = i - 1, north = i - (SIZE + 1);
				double tone = LAND;
				if(kind[i] == 2) {
					boolean shore = kind[west] == 1 || kind[north] == 1;
					tone = shore ? SHORE : (px + pz) % 2 == 0 ? WATER_DOT : LAND;
				}
				else if(kind[i] == 1) {
					boolean shore = kind[west] == 2 || kind[north] == 2;
					int band = Math.floorDiv(height[i], CONTOUR);
					boolean contour = (kind[west] == 1 && Math.floorDiv(height[west], CONTOUR) != band)
							|| (kind[north] == 1 && Math.floorDiv(height[north], CONTOUR) != band);
					tone = shore ? SHORE : contour ? CONTOUR_LINE : LAND;
				}
				if(tone == LAND && (px + pz) % 2 == 0) { // The grid, dotted, under everything else.
					int x = blockAt(fit.centerX(), px, fit.blocksPerPixel()), z = blockAt(fit.centerZ(), pz, fit.blocksPerPixel());
					if(Math.floorMod(x - fit.blocksPerPixel() / 2, grid) < fit.blocksPerPixel() || Math.floorMod(z - fit.blocksPerPixel() / 2, grid) < fit.blocksPerPixel())
						tone = GRID_DOT;
				}
				colors[px + pz * SIZE] = grey(tone);
			}
		}
		return colors;
	}

	/** The block in the middle of map pixel {@code pixel} along one axis. */
	private static int blockAt(int center, int pixel, int blocksPerPixel) {
		return center + (pixel - SIZE / 2) * blocksPerPixel + blocksPerPixel / 2;
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
