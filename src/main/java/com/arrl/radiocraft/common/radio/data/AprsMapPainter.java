package com.arrl.radiocraft.common.radio.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Paints APRS maps (vanilla maps an H19 made, see {@code AprsTerminalBlockEntity#createMaps}) in the style of an old
 * terminal: black land with contour lines, dithered water with a bright shoreline, a dotted grid, a box at the home
 * station and a cross at each station heard with its callsign beside it in a small pixel font. It is centred on them
 * all and scaled to fit, in powers of two so it does not jitter as stations come and go.
 * <p>
 * A wall of maps (2 by 2 or 3 by 3) is painted as one picture and cut into tiles, so markers and labels run across the
 * joins. Everything is in the picture itself: the maps carry no vanilla markers.
 */
public final class AprsMapPainter {

	/** Pixels across one map. */
	public static final int SIZE = 128;
	/** Fewest blocks a map shows across, so a lone station is not drawn a pixel per block. */
	public static final int MIN_SPAN = 128;
	/** Height between contour lines, in blocks. */
	public static final int CONTOUR = 8;
	// The few tones of the map, as luminance 0 (black) to 1 (white).
	private static final double LAND = 0.0D, CONTOUR_LINE = 0.42D, WATER_DOT = 0.22D, SHORE = 0.8D, GRID_DOT = 0.3D, MARK = 1.0D;

	/** Grey map colours from black to white, as packed map colour ids with their brightness 0 to 1. */
	private static final List<double[]> GREYS = new ArrayList<>();
	public static final byte BLACK;
	public static final byte WHITE;

	static {
		MapColor[] bases = { MapColor.COLOR_BLACK, MapColor.COLOR_GRAY, MapColor.DEEPSLATE, MapColor.STONE, MapColor.COLOR_LIGHT_GRAY, MapColor.WOOL, MapColor.SNOW };
		for(MapColor base : bases) {
			for(MapColor.Brightness brightness : MapColor.Brightness.values()) {
				int rgb = base.col;
				double luminance = (0.299D * ((rgb >> 16) & 0xFF) + 0.587D * ((rgb >> 8) & 0xFF) + 0.114D * (rgb & 0xFF)) / 255.0D * brightness.modifier / 255.0D;
				GREYS.add(new double[] { luminance, base.getPackedId(brightness) });
			}
		}
		BLACK = grey(LAND);
		WHITE = grey(MARK);
	}

	private AprsMapPainter() {}

	/**
	 * Where the picture looks: the block at its middle, how many blocks one pixel covers (a power of two), and its size
	 * in pixels (a map is {@link #SIZE} across; a wall is that times its columns and rows).
	 */
	public record Fit(int centerX, int centerZ, int blocksPerPixel, int width, int height) {

		/** The picture pixel (may fall outside) a world column lands on. */
		public int pixelX(double x) {
			return (int)Math.floor((x - centerX) / blocksPerPixel) + width / 2;
		}

		public int pixelZ(double z) {
			return (int)Math.floor((z - centerZ) / blocksPerPixel) + height / 2;
		}

		/** The block in the middle of picture pixel {@code px}. */
		public int blockX(int px) {
			return centerX + (px - width / 2) * blocksPerPixel + blocksPerPixel / 2;
		}

		public int blockZ(int pz) {
			return centerZ + (pz - height / 2) * blocksPerPixel + blocksPerPixel / 2;
		}
	}

	/**
	 * A picture {@code width} by {@code height} pixels centred on {@code home} and {@code stations} together, with a
	 * margin, at least {@link #MIN_SPAN} blocks across.
	 */
	public static Fit fit(BlockPos home, List<BlockPos> stations, int width, int height) {
		int minX = home.getX(), maxX = minX, minZ = home.getZ(), maxZ = minZ;
		for(BlockPos at : stations) {
			minX = Math.min(minX, at.getX());
			maxX = Math.max(maxX, at.getX());
			minZ = Math.min(minZ, at.getZ());
			maxZ = Math.max(maxZ, at.getZ());
		}
		double spanX = Math.max(MIN_SPAN, (maxX - minX) * 1.25D + 16), spanZ = Math.max(MIN_SPAN, (maxZ - minZ) * 1.25D + 16);
		int blocksPerPixel = 1;
		while((blocksPerPixel * width < spanX || blocksPerPixel * height < spanZ) && blocksPerPixel < (1 << 16))
			blocksPerPixel *= 2;
		int step = blocksPerPixel * 8; // The centre moves in whole grid steps, so the picture does not crawl.
		int centerX = Math.floorDiv((minX + maxX) / 2 + step / 2, step) * step;
		int centerZ = Math.floorDiv((minZ + maxZ) / 2 + step / 2, step) * step;
		return new Fit(centerX, centerZ, blocksPerPixel, width, height);
	}

	/** A station to mark: its callsign and where it is. */
	public record Station(String callsign, BlockPos pos) {}

	/**
	 * The picture for {@code fit}: terrain from the loaded chunks (black where nothing is loaded), then the home box and
	 * each station's cross and callsign. One packed map colour per pixel, row by row (x fastest).
	 */
	public static byte[] paint(ServerLevel level, Fit fit, BlockPos home, List<Station> stations) {
		byte[] colors = paintTerrain(level, fit);
		for(Station station : stations)
			drawStation(colors, fit, station);
		drawHome(colors, fit, fit.pixelX(home.getX() + 0.5D), fit.pixelZ(home.getZ() + 0.5D));
		return colors;
	}

	private static byte[] paintTerrain(ServerLevel level, Fit fit) {
		int w = fit.width(), h = fit.height(), stride = w + 1;
		int[] height = new int[stride * (h + 1)]; // With a row and a column of neighbours to the north and west.
		byte[] kind = new byte[stride * (h + 1)]; // 0 unknown, 1 land, 2 water.
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for(int px = -1; px < w; px++) {
			for(int pz = -1; pz < h; pz++) {
				int x = fit.blockX(px), z = fit.blockZ(pz);
				if(!level.hasChunk(x >> 4, z >> 4))
					continue;
				int i = (px + 1) + (pz + 1) * stride;
				int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
				pos.set(x, top, z);
				height[i] = top;
				kind[i] = (byte)(level.getFluidState(pos).is(FluidTags.WATER) ? 2 : 1);
			}
		}
		byte[] colors = new byte[w * h];
		int grid = gridStep(fit.blocksPerPixel());
		for(int px = 0; px < w; px++) {
			for(int pz = 0; pz < h; pz++) {
				int i = (px + 1) + (pz + 1) * stride, west = i - 1, north = i - stride;
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
					int x = fit.blockX(px), z = fit.blockZ(pz), half = fit.blocksPerPixel() / 2;
					if(Math.floorMod(x - half, grid) < fit.blocksPerPixel() || Math.floorMod(z - half, grid) < fit.blocksPerPixel())
						tone = GRID_DOT;
				}
				colors[px + pz * w] = grey(tone);
			}
		}
		return colors;
	}

	/** A cross at the station and its callsign beside it, on black so it reads over contours; flipped left near the edge. */
	private static void drawStation(byte[] colors, Fit fit, Station station) {
		int x = fit.pixelX(station.pos().getX() + 0.5D), z = fit.pixelZ(station.pos().getZ() + 0.5D);
		for(int dx = -2; dx <= 2; dx++)
			for(int dz = -2; dz <= 2; dz++)
				set(colors, fit, x + dx, z + dz, (dx == 0 || dz == 0) && Math.abs(dx + dz) <= 1 ? WHITE : BLACK);
		set(colors, fit, x, z, WHITE);
		String label = station.callsign().toUpperCase(Locale.ROOT);
		int textWidth = label.length() * 4 - 1;
		int left = x + 3 + textWidth + 1 < fit.width() ? x + 3 : x - 3 - textWidth;
		text(colors, fit, label, left, z - 2);
	}

	/** Home: a hollow box with a dot in it. */
	private static void drawHome(byte[] colors, Fit fit, int x, int z) {
		for(int dx = -3; dx <= 3; dx++)
			for(int dz = -3; dz <= 3; dz++) {
				int ring = Math.max(Math.abs(dx), Math.abs(dz));
				set(colors, fit, x + dx, z + dz, ring == 2 || ring == 0 ? WHITE : BLACK);
			}
	}

	/** {@code text} in the 3 by 5 pixel font, its top left at {@code x}, {@code z}, on a black box one pixel wider. */
	public static void text(byte[] colors, Fit fit, String text, int x, int z) {
		int width = text.length() * 4 - 1;
		for(int dx = -1; dx <= width; dx++)
			for(int dz = -1; dz <= 5; dz++)
				set(colors, fit, x + dx, z + dz, BLACK);
		for(int c = 0; c < text.length(); c++) {
			String[] glyph = FONT.getOrDefault(text.charAt(c), FONT.get('-'));
			for(int row = 0; row < 5; row++)
				for(int col = 0; col < 3; col++)
					if(glyph[row].charAt(col) == '#')
						set(colors, fit, x + c * 4 + col, z + row, WHITE);
		}
	}

	private static void set(byte[] colors, Fit fit, int x, int z, byte color) {
		if(x >= 0 && x < fit.width() && z >= 0 && z < fit.height())
			colors[x + z * fit.width()] = color;
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

	/** Puts tile {@code column}, {@code row} of the picture on {@code data}; only pixels that changed go out to players. */
	public static void applyTile(MapItemSavedData data, Fit fit, byte[] colors, int column, int row) {
		for(int px = 0; px < SIZE; px++)
			for(int pz = 0; pz < SIZE; pz++)
				data.updateColor(px, pz, colors[(column * SIZE + px) + (row * SIZE + pz) * fit.width()]);
	}

	/** A 3 by 5 pixel font for callsigns: capitals, digits, dash and slash. */
	private static final Map<Character, String[]> FONT = Map.ofEntries(
			Map.entry('A', new String[] { ".#.", "#.#", "###", "#.#", "#.#" }), Map.entry('B', new String[] { "##.", "#.#", "##.", "#.#", "##." }),
			Map.entry('C', new String[] { ".##", "#..", "#..", "#..", ".##" }), Map.entry('D', new String[] { "##.", "#.#", "#.#", "#.#", "##." }),
			Map.entry('E', new String[] { "###", "#..", "##.", "#..", "###" }), Map.entry('F', new String[] { "###", "#..", "##.", "#..", "#.." }),
			Map.entry('G', new String[] { ".##", "#..", "#.#", "#.#", ".##" }), Map.entry('H', new String[] { "#.#", "#.#", "###", "#.#", "#.#" }),
			Map.entry('I', new String[] { "###", ".#.", ".#.", ".#.", "###" }), Map.entry('J', new String[] { "..#", "..#", "..#", "#.#", ".#." }),
			Map.entry('K', new String[] { "#.#", "#.#", "##.", "#.#", "#.#" }), Map.entry('L', new String[] { "#..", "#..", "#..", "#..", "###" }),
			Map.entry('M', new String[] { "#.#", "###", "#.#", "#.#", "#.#" }), Map.entry('N', new String[] { "##.", "#.#", "#.#", "#.#", "#.#" }),
			Map.entry('O', new String[] { ".#.", "#.#", "#.#", "#.#", ".#." }), Map.entry('P', new String[] { "##.", "#.#", "##.", "#..", "#.." }),
			Map.entry('Q', new String[] { ".#.", "#.#", "#.#", "##.", ".##" }), Map.entry('R', new String[] { "##.", "#.#", "##.", "#.#", "#.#" }),
			Map.entry('S', new String[] { ".##", "#..", ".#.", "..#", "##." }), Map.entry('T', new String[] { "###", ".#.", ".#.", ".#.", ".#." }),
			Map.entry('U', new String[] { "#.#", "#.#", "#.#", "#.#", "###" }), Map.entry('V', new String[] { "#.#", "#.#", "#.#", "#.#", ".#." }),
			Map.entry('W', new String[] { "#.#", "#.#", "###", "###", "#.#" }), Map.entry('X', new String[] { "#.#", "#.#", ".#.", "#.#", "#.#" }),
			Map.entry('Y', new String[] { "#.#", "#.#", ".#.", ".#.", ".#." }), Map.entry('Z', new String[] { "###", "..#", ".#.", "#..", "###" }),
			Map.entry('0', new String[] { "###", "#.#", "#.#", "#.#", "###" }), Map.entry('1', new String[] { ".#.", "##.", ".#.", ".#.", "###" }),
			Map.entry('2', new String[] { "##.", "..#", ".#.", "#..", "###" }), Map.entry('3', new String[] { "##.", "..#", ".#.", "..#", "##." }),
			Map.entry('4', new String[] { "#.#", "#.#", "###", "..#", "..#" }), Map.entry('5', new String[] { "###", "#..", "##.", "..#", "##." }),
			Map.entry('6', new String[] { ".##", "#..", "###", "#.#", "###" }), Map.entry('7', new String[] { "###", "..#", ".#.", ".#.", ".#." }),
			Map.entry('8', new String[] { "###", "#.#", "###", "#.#", "###" }), Map.entry('9', new String[] { "###", "#.#", "###", "..#", "##." }),
			Map.entry('-', new String[] { "...", "...", "###", "...", "..." }), Map.entry('/', new String[] { "..#", "..#", ".#.", "#..", "#.." }));
}
