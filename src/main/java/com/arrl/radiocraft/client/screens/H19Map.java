package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.data.AprsIcon;
import com.arrl.radiocraft.common.radio.data.AprsMapPainter;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The H19's map page: the APRS map of the framed maps redrawn in green phosphor, finer (two pixels to a screen texel)
 * and with more on it: contour lines with brighter index contours every fourth, dithered water with a bright
 * shoreline, stippled tree tops, a dotted grid, and a 7 by 7 icon for each station's APRS symbol. Terrain comes from
 * the chunks the client has loaded; beyond them is a faint dot pattern. It paints a few rows a frame, top to bottom,
 * like an old screen drawing, and again every so often.
 */
public class H19Map implements AutoCloseable {

	public static final ResourceLocation LOCATION = Radiocraft.id("dynamic/h19_map");
	/** Rows painted each frame. */
	private static final int ROWS_PER_FRAME = 10;
	private static final long REPAINT_MILLIS = 10_000L;

	// Phosphor tones, ARGB.
	public static final int BLACK = 0xFF000400;
	private static final int UNKNOWN = 0xFF0A2C08;
	private static final int GRID = 0xFF0C3E09;
	private static final int CONTOUR = 0xFF14800D;
	private static final int INDEX_CONTOUR = 0xFF22C016;
	private static final int WATER = 0xFF0F5C0B;
	private static final int TREES = 0xFF0D4C0A;
	public static final int BRIGHT = 0xFF39FF14;

	private static final byte UNLOADED = 0, LAND = 1, WATER_KIND = 2, TREE_KIND = 3;

	/** A station on the map: its callsign, where it is, its symbol; home is this terminal, selected is highlighted. */
	public record Mark(String callsign, BlockPos pos, AprsIcon icon, boolean home, boolean selected) {}

	private final int width;
	private final int height;
	private final DynamicTexture texture;
	private final int[] terrain;
	private final int[] heights;
	private final byte[] kinds;

	private double centerX, centerZ, blocksPerPixel = 1.0D;
	private int paintRow;
	private long paintedAt;
	private List<Mark> composedMarks = null;
	private boolean dirty = true;

	public H19Map(int width, int height) {
		this.width = width;
		this.height = height;
		this.texture = new DynamicTexture(width, height, false);
		this.terrain = new int[width * height];
		this.heights = new int[width * height];
		this.kinds = new byte[width * height];
		java.util.Arrays.fill(terrain, BLACK);
		Minecraft.getInstance().getTextureManager().register(LOCATION, texture);
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public double blocksPerPixel() {
		return blocksPerPixel;
	}

	public double centerX() {
		return centerX;
	}

	public double centerZ() {
		return centerZ;
	}

	/** Looks at {@code centerX}, {@code centerZ} with one pixel covering {@code blocksPerPixel} blocks; repaints if it moved. */
	public void view(double centerX, double centerZ, double blocksPerPixel) {
		if(centerX == this.centerX && centerZ == this.centerZ && blocksPerPixel == this.blocksPerPixel)
			return;
		this.centerX = centerX;
		this.centerZ = centerZ;
		this.blocksPerPixel = blocksPerPixel;
		paintRow = 0;
	}

	/** True while the terrain is still being drawn in; {@link #paintRow} is where. */
	public boolean isPainting() {
		return paintRow < height;
	}

	public int paintRow() {
		return paintRow;
	}

	/** The world column under map pixel {@code px}. */
	public int blockX(double px) {
		return (int)Math.floor(centerX + (px + 0.5D - width / 2.0D) * blocksPerPixel);
	}

	public int blockZ(double py) {
		return (int)Math.floor(centerZ + (py + 0.5D - height / 2.0D) * blocksPerPixel);
	}

	/** The map pixel (may be off the map) a world position lands on. */
	public int pixelX(double x) {
		return (int)Math.floor((x - centerX) / blocksPerPixel + width / 2.0D);
	}

	public int pixelZ(double z) {
		return (int)Math.floor((z - centerZ) / blocksPerPixel + height / 2.0D);
	}

	/**
	 * Paints the next rows of terrain and puts the marks on. True if it painted rows this frame (for the clicking).
	 */
	public boolean update(ClientLevel level, List<Mark> marks) {
		long now = System.currentTimeMillis();
		if(!isPainting() && now - paintedAt > REPAINT_MILLIS)
			paintRow = 0;
		boolean painted = false;
		if(isPainting()) {
			int end = Math.min(height, paintRow + ROWS_PER_FRAME);
			for(int py = paintRow; py < end; py++)
				paintRow(level, py);
			paintRow = end;
			if(!isPainting())
				paintedAt = now;
			painted = dirty = true;
		}
		if(dirty || !marks.equals(composedMarks)) {
			compose(marks);
			composedMarks = List.copyOf(marks);
			dirty = false;
		}
		return painted;
	}

	private void paintRow(ClientLevel level, int py) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int contour = blocksPerPixel <= 1.0D ? 4 : blocksPerPixel <= 4.0D ? 8 : 16;
		int grid = 16;
		while(grid < blocksPerPixel * 48)
			grid *= 2;
		for(int px = 0; px < width; px++) {
			int i = px + py * width;
			int x = blockX(px), z = blockZ(py);
			byte kind = UNLOADED;
			int top = 0;
			if(level.hasChunk(x >> 4, z >> 4)) {
				top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
				pos.set(x, top, z);
				kind = level.getFluidState(pos).is(FluidTags.WATER) ? WATER_KIND : level.getBlockState(pos).is(BlockTags.LEAVES) ? TREE_KIND : LAND;
			}
			kinds[i] = kind;
			heights[i] = top;
			int tone = BLACK;
			if(kind == UNLOADED)
				tone = px % 4 == 0 && py % 4 == 0 ? UNKNOWN : BLACK;
			else if(kind == WATER_KIND) {
				boolean shore = neighbourKind(px - 1, py) == LAND || neighbourKind(px, py - 1) == LAND;
				tone = shore ? BRIGHT : (px + py) % 2 == 0 ? WATER : BLACK;
			}
			else {
				boolean shore = neighbourKind(px - 1, py) == WATER_KIND || neighbourKind(px, py - 1) == WATER_KIND;
				int band = Math.floorDiv(top, contour);
				int line = Math.max(contourAgainst(px - 1, py, band, contour, kind), contourAgainst(px, py - 1, band, contour, kind));
				if(shore)
					tone = BRIGHT;
				else if(line > 0)
					tone = line == 2 ? INDEX_CONTOUR : CONTOUR;
				else if(kind == TREE_KIND && (px + 2 * py) % 3 == 0)
					tone = TREES;
			}
			if(tone == BLACK && (px + py) % 2 == 0 && (Math.floorMod(x, grid) < blocksPerPixel || Math.floorMod(z, grid) < blocksPerPixel))
				tone = GRID;
			terrain[i] = tone;
		}
	}

	private byte neighbourKind(int px, int py) {
		return px < 0 || py < 0 ? UNLOADED : kinds[px + py * width];
	}

	/** 0 if no contour runs between here and that pixel; 1 for a contour, 2 for an index contour (every fourth). */
	private int contourAgainst(int px, int py, int band, int contour, byte kind) {
		if(px < 0 || py < 0)
			return 0;
		int i = px + py * width;
		if(kinds[i] != LAND || kind != LAND)
			return 0; // Tree tops stand above the ground; no contours through woods.
		int other = Math.floorDiv(heights[i], contour);
		if(other == band)
			return 0;
		return Math.floorMod(Math.max(band, other), 4) == 0 ? 2 : 1;
	}

	/** Terrain, then the scan line while painting, then the stations, north arrow and scale bar. */
	private void compose(List<Mark> marks) {
		NativeImage image = texture.getPixels();
		if(image == null)
			return;
		int[] out = terrain.clone();
		if(isPainting())
			for(int px = 0; px < width; px++)
				out[px + paintRow * width] = INDEX_CONTOUR;
		for(Mark mark : marks)
			if(!mark.selected())
				drawMark(out, mark);
		for(Mark mark : marks) // The selected one on top.
			if(mark.selected())
				drawMark(out, mark);
		drawNorth(out, width - 9, 3);
		drawScale(out, 4, height - 9);
		for(int py = 0; py < height; py++)
			for(int px = 0; px < width; px++)
				image.setPixelRGBA(px, py, abgr(out[px + py * width]));
		texture.upload();
	}

	private void drawMark(int[] out, Mark mark) {
		int x = pixelX(mark.pos().getX() + 0.5D), z = pixelZ(mark.pos().getZ() + 0.5D);
		String[] icon = ICONS.get(mark.icon());
		int ink = mark.selected() ? BLACK : BRIGHT, ground = mark.selected() ? BRIGHT : BLACK;
		for(int dx = -4; dx <= 4; dx++)
			for(int dz = -4; dz <= 4; dz++)
				set(out, x + dx, z + dz, ground);
		if(mark.home()) // A ring round home.
			for(int d = -5; d <= 5; d++) {
				set(out, x + d, z - 5, BRIGHT);
				set(out, x + d, z + 5, BRIGHT);
				set(out, x - 5, z + d, BRIGHT);
				set(out, x + 5, z + d, BRIGHT);
			}
		for(int row = 0; row < 7; row++)
			for(int col = 0; col < 7; col++)
				if(icon[row].charAt(col) == '#')
					set(out, x - 3 + col, z - 3 + row, ink);
		String label = mark.callsign().toUpperCase(Locale.ROOT);
		int textWidth = label.length() * 4 - 1;
		int left = x + 7 + textWidth < width ? x + 7 : x - 7 - textWidth;
		text(out, label, left, z - 2, mark.selected());
	}

	/** {@code text} in the 3 by 5 font on a black box; inverted (black on green) if {@code inverse}. */
	private void text(int[] out, String text, int x, int z, boolean inverse) {
		int ink = inverse ? BLACK : BRIGHT, ground = inverse ? BRIGHT : BLACK;
		int textWidth = text.length() * 4 - 1;
		for(int dx = -1; dx <= textWidth; dx++)
			for(int dz = -1; dz <= 5; dz++)
				set(out, x + dx, z + dz, ground);
		for(int c = 0; c < text.length(); c++) {
			if(text.charAt(c) == ' ')
				continue;
			String[] glyph = AprsMapPainter.glyph(text.charAt(c));
			for(int row = 0; row < 5; row++)
				for(int col = 0; col < 3; col++)
					if(glyph[row].charAt(col) == '#')
						set(out, x + c * 4 + col, z + row, ink);
		}
	}

	private void drawNorth(int[] out, int x, int z) {
		String[] arrow = { "..#..", ".###.", "#.#.#", "..#..", "..#.." };
		for(int row = 0; row < 5; row++)
			for(int col = 0; col < 5; col++)
				if(arrow[row].charAt(col) == '#')
					set(out, x + col, z + row, BRIGHT);
		text(out, "N", x + 1, z + 7, false);
	}

	/** A bar 32 pixels long with the distance it covers. */
	private void drawScale(int[] out, int x, int z) {
		int length = 32;
		for(int d = 0; d <= length; d++)
			set(out, x + d, z + 6, BRIGHT);
		set(out, x, z + 5, BRIGHT);
		set(out, x + length, z + 5, BRIGHT);
		double blocks = length * blocksPerPixel;
		text(out, blocks >= 1000 ? String.format(Locale.ROOT, "%.1fK", blocks / 1000.0D) : (int)blocks + "M", x + 2, z - 1, false);
	}

	private void set(int[] out, int x, int z, int colour) {
		if(x >= 0 && x < width && z >= 0 && z < height)
			out[x + z * width] = colour;
	}

	/** NativeImage keeps pixels as ABGR. */
	private static int abgr(int argb) {
		return (argb & 0xFF00FF00) | (argb & 0xFF) << 16 | (argb >> 16) & 0xFF;
	}

	@Override
	public void close() {
		Minecraft.getInstance().getTextureManager().release(LOCATION);
	}

	/** Black and white 7 by 7 symbols for the APRS icons. */
	private static final Map<AprsIcon, String[]> ICONS = new EnumMap<>(AprsIcon.class);

	static {
		ICONS.put(AprsIcon.HOUSE, new String[] { "...#...", "..###..", ".#####.", "#######", ".##.##.", ".##.##.", ".##.##." });
		ICONS.put(AprsIcon.PORTABLE, new String[] { "...#...", "..###..", "...#...", ".#####.", "...#...", "..#.#..", ".#...#." });
		ICONS.put(AprsIcon.CAR, new String[] { ".......", "..###..", ".#...#.", "#######", "#######", ".#...#.", "......." });
		ICONS.put(AprsIcon.BOAT, new String[] { "...#...", "...##..", "...###.", "...#...", "#######", ".#####.", "..###.." });
		ICONS.put(AprsIcon.HORSE, new String[] { "##.....", "###....", ".######", "..#####", "..#...#", "..#...#", "......." });
		ICONS.put(AprsIcon.CAMP, new String[] { "...#...", "..#.#..", "..#.#..", ".#.#.#.", ".#.#.#.", "#..#..#", "#######" });
		ICONS.put(AprsIcon.TOWER, new String[] { "#..#..#", ".#.#.#.", "...#...", "..###..", "..#.#..", ".#.#.#.", ".#...#." });
		ICONS.put(AprsIcon.DIGI, new String[] { "...#...", "..#.#..", ".#.#.#.", "#.###.#", ".#.#.#.", "..#.#..", "...#..." });
	}
}
