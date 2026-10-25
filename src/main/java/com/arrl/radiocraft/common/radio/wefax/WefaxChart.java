package com.arrl.radiocraft.common.radio.wefax;

import com.arrl.radiocraft.api.weather.StormReport;
import com.arrl.radiocraft.api.weather.WeatherReport;
import com.arrl.radiocraft.api.weather.WeatherSource;
import com.arrl.radiocraft.common.radio.data.AprsMapPainter;
import com.arrl.radiocraft.common.radio.weather.WeatherReports;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Paints WEFAX weather charts onto a vanilla map's 128 by 128 pixels, in the style of a radiofacsimile print: black
 * ink on white paper, a header band with the chart's name, station and time, coastlines, a dotted grid, and the
 * weather drawn in dither patterns and line work. Three charts:
 * <ul>
 * <li>{@link Type#RADAR}: rain as dot screens from light to solid black, with hail cores hatched, and a symbol, label
 * and 5-minute motion arrow for each storm (storms here cross the chart in about 20 minutes).</li>
 * <li>{@link Type#FORECAST}: each storm's track an hour ahead (or to the forecast's end), with its outline now and at
 * the end, storms forecast to form drawn dashed with the minutes until they do, and the next arrival at the station.</li>
 * <li>{@link Type#SURFACE}: isobars every 4 hectopascals, low and high pressure centres, and wind arrows.</li>
 * </ul>
 * Everything comes from a {@link WeatherSource}, so it works with vanilla weather or a weather mod. The station is at
 * the centre.
 */
public final class WefaxChart {

	/** The charts a WEFAX station sends. */
	public enum Type {
		RADAR("RADAR"), FORECAST("FCST +60M"), SURFACE("SFC ANALYSIS");

		private final String title;

		Type(String title) {
			this.title = title;
		}

		public String title() {
			return title;
		}
	}

	public static final int SIZE = AprsMapPainter.SIZE;
	/** Header band: rows 0 to 8, a rule at row 9. */
	static final int HEADER = 10;
	/** Footer band: the last 9 rows, a rule above. */
	static final int FOOTER = 9;
	static final int CHART_TOP = HEADER;
	static final int CHART_BOTTOM = SIZE - FOOTER - 1;

	public static final byte PAPER = AprsMapPainter.grey(1.0D);
	public static final byte INK = AprsMapPainter.grey(0.0D);
	static final byte SMUDGE = AprsMapPainter.grey(0.62D);

	/** Rain thresholds for the radar's four screens. */
	static final double[] RAIN_LEVELS = { 0.2, 0.45, 0.7, 0.9 };
	/** Minutes of motion a radar storm's arrow shows. */
	static final int VECTOR_MINUTES = 5;
	/** Hectopascals between isobars. */
	static final int ISOBAR = 4;
	/** Minutes ahead the forecast chart looks. */
	static final int FORECAST_MINUTES = 60;

	private WefaxChart() {
	}

	/**
	 * How the chart covers the ground: the centre and blocks per pixel. 16 blocks a pixel (2 km across) unless a
	 * hurricane is about, when it zooms out to 32.
	 */
	public record Frame(int centerX, int centerZ, int blocksPerPixel) {
		public double blockX(int px) {
			return centerX + (px - SIZE / 2 + 0.5) * blocksPerPixel;
		}

		public double blockZ(int pz) {
			return centerZ + (pz - (CHART_TOP + CHART_BOTTOM) / 2 + 0.5) * blocksPerPixel;
		}

		public int pixelX(double x) {
			return (int)Math.floor((x - centerX) / blocksPerPixel) + SIZE / 2;
		}

		public int pixelZ(double z) {
			return (int)Math.floor((z - centerZ) / blocksPerPixel) + (CHART_TOP + CHART_BOTTOM) / 2;
		}
	}

	public static Frame frame(BlockPos center, List<StormReport> storms) {
		boolean hurricane = storms.stream().anyMatch(s -> s.kind() == StormReport.Kind.HURRICANE && !s.worldWide());
		return new Frame(center.getX(), center.getZ(), hurricane ? 32 : 16);
	}

	/**
	 * The chart, one packed map colour per pixel, row by row (x fastest).
	 *
	 * @param station the sending station's callsign, for the header (may be empty)
	 */
	public static byte[] paint(ServerLevel level, WeatherSource source, BlockPos center, Type type, String station) {
		long now = level.getGameTime();
		List<StormReport> storms = source.storms(level, center, 1500, type == Type.FORECAST);
		Frame frame = frame(center, storms);
		Canvas canvas = new Canvas();
		canvas.fill(PAPER);
		coast(level, canvas, frame);
		grid(canvas, frame);
		switch(type) {
			case RADAR -> radar(level, source, canvas, frame, storms, now);
			case FORECAST -> forecast(level, source, canvas, frame, storms, center, now);
			case SURFACE -> surface(level, source, canvas, frame, now);
		}
		stationMark(canvas, frame, center);
		header(canvas, type, station, WeatherReports.timestamp(level.getDayTime()));
		footer(level, source, canvas, frame, type, center, now);
		return canvas.pixels;
	}

	// ---------------------------------------------------------------- Base map

	/** Coastlines from the biome source (no chunks are loaded), and a faint screen over the sea. */
	static void coast(ServerLevel level, Canvas canvas, Frame frame) {
		int rows = CHART_BOTTOM - CHART_TOP + 1;
		boolean[] sea = new boolean[SIZE * rows];
		var generator = level.getChunkSource().getGenerator().getBiomeSource();
		var sampler = level.getChunkSource().randomState().sampler();
		int qy = QuartPos.fromBlock(level.getSeaLevel());
		for(int pz = 0; pz < rows; pz++)
			for(int px = 0; px < SIZE; px++) {
				Holder<Biome> biome = generator.getNoiseBiome(QuartPos.fromBlock((int)frame.blockX(px)), qy,
						QuartPos.fromBlock((int)frame.blockZ(pz + CHART_TOP)), sampler);
				sea[px + pz * SIZE] = biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER) && frame.blocksPerPixel() <= 8;
			}
		for(int pz = 0; pz < rows; pz++)
			for(int px = 0; px < SIZE; px++) {
				boolean here = sea[px + pz * SIZE];
				boolean edge = px + 1 < SIZE && sea[px + 1 + pz * SIZE] != here || pz + 1 < rows && sea[px + (pz + 1) * SIZE] != here;
				if(edge)
					canvas.set(px, pz + CHART_TOP, INK);
				else if(here && (px % 4 == 0 && (pz + CHART_TOP) % 4 == 2))
					canvas.set(px, pz + CHART_TOP, SMUDGE);
			}
	}

	/** A dotted grid at round numbers of blocks, labelled along the left and bottom. */
	static void grid(Canvas canvas, Frame frame) {
		int step = frame.blocksPerPixel() * 32;
		for(int px = 0; px < SIZE; px++) {
			double x = frame.blockX(px);
			if(Math.floorMod((int)Math.floor(x), step) < frame.blocksPerPixel())
				for(int pz = CHART_TOP; pz <= CHART_BOTTOM; pz += 3)
					canvas.set(px, pz, INK);
		}
		for(int pz = CHART_TOP; pz <= CHART_BOTTOM; pz++) {
			double z = frame.blockZ(pz);
			if(Math.floorMod((int)Math.floor(z), step) < frame.blocksPerPixel())
				for(int px = 0; px < SIZE; px += 3)
					canvas.set(px, pz, INK);
		}
	}

	static void stationMark(Canvas canvas, Frame frame, BlockPos center) {
		int x = frame.pixelX(center.getX() + 0.5), z = frame.pixelZ(center.getZ() + 0.5);
		canvas.ring(x, z, 3, PAPER);
		canvas.ring(x, z, 2, INK);
		canvas.set(x, z, INK);
	}

	// ---------------------------------------------------------------- Radar

	static void radar(ServerLevel level, WeatherSource source, Canvas canvas, Frame frame, List<StormReport> storms, long now) {
		int rows = CHART_BOTTOM - CHART_TOP + 1;
		byte[] band = new byte[SIZE * rows];
		for(int pz = 0; pz < rows; pz++)
			for(int px = 0; px < SIZE; px++) {
				double rain = source.rainAt(level, frame.blockX(px), frame.blockZ(pz + CHART_TOP), now);
				int b = 0;
				while(b < RAIN_LEVELS.length && rain > RAIN_LEVELS[b])
					b++;
				band[px + pz * SIZE] = (byte)b;
			}
		for(int pz = 0; pz < rows; pz++)
			for(int px = 0; px < SIZE; px++) {
				int b = band[px + pz * SIZE];
				int y = pz + CHART_TOP;
				boolean edge = b > 0 && (px == 0 || band[px - 1 + pz * SIZE] < b || pz == 0 || band[px + (pz - 1) * SIZE] < b
						|| px + 1 == SIZE || band[px + 1 + pz * SIZE] < b || pz + 1 == rows || band[px + (pz + 1) * SIZE] < b);
				if(b > 0 && (edge && b == 1 || screen(b, px, y)))
					canvas.set(px, y, INK);
			}
		for(StormReport storm : storms) {
			if(storm.worldWide() || !storm.formed(now))
				continue;
			StormReport.TrackPoint at = storm.at(now);
			int x = frame.pixelX(at.x()), z = frame.pixelZ(at.z());
			if(storm.kind() == StormReport.Kind.SUPERCELL && at.intensity() > 0.7)
				hailCore(canvas, x, z, (int)Math.max(2, at.radius() * 0.3 / frame.blocksPerPixel()));
			StormReport.TrackPoint later = storm.at(now + VECTOR_MINUTES * 1200L);
			canvas.arrow(x, z, frame.pixelX(later.x()), frame.pixelZ(later.z()), INK);
			symbol(canvas, storm.kind(), x, z);
			canvas.label("S" + storm.id(), x + 5, z - 7);
		}
	}

	/** The dot screen for rain band {@code b}: a quarter, half, three quarters, then solid. */
	static boolean screen(int b, int x, int y) {
		return switch(b) {
			case 1 -> (x & 1) == 0 && (y & 1) == 0 && ((x >> 1) + (y >> 1) & 1) == 0;
			case 2 -> (x + y & 1) == 0;
			case 3 -> !((x & 1) == 0 && (y & 1) == 0);
			default -> true;
		};
	}

	/** Cross-hatching (white on the black core) where hail is likely. */
	static void hailCore(Canvas canvas, int cx, int cz, int r) {
		for(int dz = -r; dz <= r; dz++)
			for(int dx = -r; dx <= r; dx++)
				if(dx * dx + dz * dz <= r * r && ((cx + dx + cz + dz) % 3 == 0 || (cx + dx - cz - dz) % 3 == 0))
					canvas.set(cx + dx, cz + dz, PAPER);
	}

	/** A weather-map symbol, on a little paper patch so it reads over the screens. */
	static void symbol(Canvas canvas, StormReport.Kind kind, int x, int z) {
		String[] glyph = switch(kind) {
			// Shower: a triangle point down. Thunderstorm: the bolt. Hurricane: a ring with two arms.
			case SHOWER -> new String[] { "#####", ".###.", "..#..", "", "" };
			case THUNDERSTORM, SEVERE_THUNDERSTORM, SUPERCELL -> new String[] { "####.", "...#.", "..#..", ".#...", "#...." };
			case HURRICANE -> new String[] { "..##.", ".###.", "##.##", ".###.", ".##.." };
			case OTHER -> new String[] { ".###.", "#...#", "#...#", "#...#", ".###." };
		};
		for(int dz = -3; dz <= 3; dz++)
			for(int dx = -3; dx <= 3; dx++)
				canvas.set(x + dx, z + dz, PAPER);
		for(int row = 0; row < glyph.length; row++)
			for(int col = 0; col < glyph[row].length(); col++)
				if(glyph[row].charAt(col) == '#')
					canvas.set(x - 2 + col, z - 2 + row, INK);
	}

	// ---------------------------------------------------------------- Forecast

	static void forecast(ServerLevel level, WeatherSource source, Canvas canvas, Frame frame, List<StormReport> storms,
			BlockPos center, long now) {
		long end = now + Math.min(FORECAST_MINUTES * 1200L, source.forecastHorizon(level));
		for(StormReport storm : storms) {
			if(storm.worldWide())
				continue;
			StormReport.TrackPoint first = storm.at(Math.max(now, storm.formsAt()));
			StormReport.TrackPoint last = storm.at(end);
			int x0 = frame.pixelX(first.x()), z0 = frame.pixelZ(first.z());
			int x1 = frame.pixelX(last.x()), z1 = frame.pixelZ(last.z());
			boolean formed = storm.formed(now);
			// The track, dotted, with a tick every 15 minutes.
			long from = Math.max(now, storm.formsAt());
			int px = x0, pz = z0;
			for(long t = from; t <= end; t += 1200) {
				StormReport.TrackPoint p = storm.at(t);
				int x = frame.pixelX(p.x()), z = frame.pixelZ(p.z());
				canvas.dotted(px, pz, x, z, INK);
				if((t - now) % (15 * 1200) < 1200)
					canvas.square(x, z, 1, INK);
				px = x;
				pz = z;
			}
			int r0 = (int)Math.round(first.radius() / frame.blocksPerPixel());
			int r1 = (int)Math.round(last.radius() / frame.blocksPerPixel());
			if(formed)
				canvas.ring(x0, z0, r0, INK);
			else
				canvas.dashedRing(x0, z0, r0, INK);
			canvas.dashedRing(x1, z1, r1, INK);
			symbol(canvas, storm.kind(), x1, z1);
			String label = formed ? "S" + storm.id() : "F+" + Math.max(1, (storm.formsAt() - now) / 1200) + "M";
			canvas.label(label, x0 + 4, z0 - 8);
		}
	}

	// ---------------------------------------------------------------- Surface analysis

	static void surface(ServerLevel level, WeatherSource source, Canvas canvas, Frame frame, long now) {
		int step = 8;
		int cols = SIZE / step + 1;
		int rows = (CHART_BOTTOM - CHART_TOP) / step + 2;
		double[] pressure = new double[cols * rows];
		WeatherReport[] reports = new WeatherReport[cols * rows];
		for(int j = 0; j < rows; j++)
			for(int i = 0; i < cols; i++) {
				BlockPos pos = BlockPos.containing(frame.blockX(i * step), level.getSeaLevel(), frame.blockZ(CHART_TOP + j * step));
				reports[i + j * cols] = source.reportAt(level, pos, now);
				pressure[i + j * cols] = reports[i + j * cols].pressure();
			}
		// Isobars: wherever the pressure band changes between neighbouring pixels.
		int chartRows = CHART_BOTTOM - CHART_TOP + 1;
		int[] bands = new int[SIZE * chartRows];
		for(int pz = 0; pz < chartRows; pz++)
			for(int px = 0; px < SIZE; px++) {
				double fx = px / (double)step, fz = pz / (double)step;
				int i = Math.min(cols - 2, (int)fx), j = Math.min(rows - 2, (int)fz);
				double tx = fx - i, tz = fz - j;
				double p = lerp(lerp(pressure[i + j * cols], pressure[i + 1 + j * cols], tx),
						lerp(pressure[i + (j + 1) * cols], pressure[i + 1 + (j + 1) * cols], tx), tz);
				bands[px + pz * SIZE] = (int)Math.floor(p / ISOBAR);
			}
		for(int pz = 0; pz < chartRows; pz++)
			for(int px = 0; px < SIZE; px++) {
				int b = bands[px + pz * SIZE];
				if(px + 1 < SIZE && bands[px + 1 + pz * SIZE] != b || pz + 1 < chartRows && bands[px + (pz + 1) * SIZE] != b)
					canvas.set(px, pz + CHART_TOP, INK);
			}
		// The lowest and highest points, marked L and H with their pressure, where they stand out from the mean.
		int low = 0, high = 0;
		double mean = 0;
		for(int k = 0; k < pressure.length; k++) {
			mean += pressure[k] / pressure.length;
			if(pressure[k] < pressure[low])
				low = k;
			if(pressure[k] > pressure[high])
				high = k;
		}
		if(mean - pressure[low] >= 2)
			centre(canvas, "L", pressure[low], (low % cols) * step, CHART_TOP + (low / cols) * step);
		if(pressure[high] - mean >= 2)
			centre(canvas, "H", pressure[high], (high % cols) * step, CHART_TOP + (high / cols) * step);
		// Wind arrows every 16 pixels, flying with the wind, one barb per 10 m/s.
		for(int j = 1; j < rows - 1; j += 2)
			for(int i = 1; i < cols - 1; i += 2) {
				WeatherReport r = reports[i + j * cols];
				if(r.windSpeed() < 1)
					continue;
				double toward = Math.toRadians(r.windFrom() + 180);
				double len = Math.min(7, 2 + r.windSpeed() / 4);
				int x = i * step, z = CHART_TOP + j * step;
				int tx = (int)Math.round(x + Math.sin(toward) * len), tz = (int)Math.round(z - Math.cos(toward) * len);
				canvas.arrow(x, z, tx, tz, INK);
				for(int barb = 0; barb < Math.min(3, (int)(r.windSpeed() / 10)); barb++) {
					double bx = x + Math.sin(toward) * barb * 2, bz = z - Math.cos(toward) * barb * 2;
					double side = toward + Math.PI / 2;
					canvas.line((int)Math.round(bx), (int)Math.round(bz), (int)Math.round(bx + Math.sin(side) * 2),
							(int)Math.round(bz - Math.cos(side) * 2), INK);
				}
			}
	}

	/** "L" over its pressure, kept inside the chart. */
	private static void centre(Canvas canvas, String letter, double pressure, int x, int z) {
		String value = String.valueOf(Math.round(pressure));
		int w = Canvas.width(value);
		int cx = Math.max(w / 2 + 1, Math.min(SIZE - w / 2 - 2, x));
		int cz = Math.max(CHART_TOP + 4, Math.min(CHART_BOTTOM - 9, z));
		canvas.label(letter, cx - 1, cz - 3);
		canvas.label(value, cx - w / 2, cz + 3);
	}

	private static double lerp(double a, double b, double t) {
		return a + (b - a) * t;
	}

	// ---------------------------------------------------------------- Header and footer

	static void header(Canvas canvas, Type type, String station, String time) {
		for(int y = 0; y < HEADER - 1; y++)
			for(int x = 0; x < SIZE; x++)
				canvas.set(x, y, PAPER);
		for(int x = 0; x < SIZE; x++)
			canvas.set(x, HEADER - 1, INK);
		canvas.text(type.title(), 2, 2, INK);
		String right = (station.isEmpty() ? "" : station + " ") + time;
		canvas.text(right, SIZE - 2 - Canvas.width(right), 2, INK);
	}

	static void footer(ServerLevel level, WeatherSource source, Canvas canvas, Frame frame, Type type, BlockPos center, long now) {
		int top = SIZE - FOOTER;
		for(int y = top; y < SIZE; y++)
			for(int x = 0; x < SIZE; x++)
				canvas.set(x, y, PAPER);
		for(int x = 0; x < SIZE; x++)
			canvas.set(x, top, INK);
		String text = switch(type) {
			case RADAR -> {
				// A legend: the four screens.
				for(int b = 1; b <= 4; b++)
					for(int dy = 0; dy < 5; dy++)
						for(int dx = 0; dx < 6; dx++)
							if(screen(b, 2 + (b - 1) * 8 + dx, top + 2 + dy))
								canvas.set(2 + (b - 1) * 8 + dx, top + 2 + dy, INK);
				yield "LGT-EXT " + frame.blocksPerPixel() * 32 + "/DIV";
			}
			case FORECAST -> {
				Optional<WeatherSource.Arrival> next = source.nextArrival(level, center, source.forecastHorizon(level));
				yield next.map(a -> a.arrivalTick() <= now ? "WX OVERHEAD"
						: "ARR " + a.storm().kind().abbreviation() + " " + WeatherReports.duration(a.arrivalTick() - now))
						.orElse("NO WX EXPECTED");
			}
			case SURFACE -> "ISOBARS " + ISOBAR + " HPA " + frame.blocksPerPixel() * 32 + "/DIV";
		};
		canvas.text(text, type == Type.RADAR ? 36 : 2, top + 2, INK);
	}

	// ---------------------------------------------------------------- Drawing

	/** A 128 by 128 page of map colours, with the few drawing tools the charts need. */
	static final class Canvas {
		final byte[] pixels = new byte[SIZE * SIZE];

		void fill(byte color) {
			java.util.Arrays.fill(pixels, color);
		}

		void set(int x, int z, byte color) {
			if(x >= 0 && x < SIZE && z >= 0 && z < SIZE)
				pixels[x + z * SIZE] = color;
		}

		byte get(int x, int z) {
			return pixels[x + z * SIZE];
		}

		void line(int x0, int z0, int x1, int z1, byte color) {
			plot(x0, z0, x1, z1, color, 1);
		}

		void dotted(int x0, int z0, int x1, int z1, byte color) {
			plot(x0, z0, x1, z1, color, 2);
		}

		/** Bresenham, setting every {@code every}th pixel. */
		private void plot(int x0, int z0, int x1, int z1, byte color, int every) {
			int dx = Math.abs(x1 - x0), dz = -Math.abs(z1 - z0);
			int sx = x0 < x1 ? 1 : -1, sz = z0 < z1 ? 1 : -1;
			int err = dx + dz, n = 0;
			for(int guard = 0; guard < 512; guard++) {
				if(n++ % every == 0)
					set(x0, z0, color);
				if(x0 == x1 && z0 == z1)
					return;
				int e2 = 2 * err;
				if(e2 >= dz) {
					err += dz;
					x0 += sx;
				}
				if(e2 <= dx) {
					err += dx;
					z0 += sz;
				}
			}
		}

		/** A line with a small arrowhead at its end. */
		void arrow(int x0, int z0, int x1, int z1, byte color) {
			line(x0, z0, x1, z1, color);
			double angle = Math.atan2(z1 - z0, x1 - x0);
			if(x0 == x1 && z0 == z1)
				return;
			for(double side : new double[] { 2.5, -2.5 }) {
				double a = angle + Math.PI + side * 0.3;
				line(x1, z1, (int)Math.round(x1 + Math.cos(a) * 3), (int)Math.round(z1 + Math.sin(a) * 3), color);
			}
		}

		void ring(int cx, int cz, int r, byte color) {
			ring(cx, cz, r, color, false);
		}

		void dashedRing(int cx, int cz, int r, byte color) {
			ring(cx, cz, r, color, true);
		}

		private void ring(int cx, int cz, int r, byte color, boolean dashed) {
			if(r <= 0) {
				set(cx, cz, color);
				return;
			}
			int steps = Math.max(8, (int)(r * 2 * Math.PI));
			for(int i = 0; i < steps; i++) {
				if(dashed && (i / 2) % 2 == 1)
					continue;
				double a = Math.PI * 2 * i / steps;
				set((int)Math.round(cx + Math.cos(a) * r), (int)Math.round(cz + Math.sin(a) * r), color);
			}
		}

		void square(int cx, int cz, int r, byte color) {
			for(int dz = -r; dz <= r; dz++)
				for(int dx = -r; dx <= r; dx++)
					set(cx + dx, cz + dz, color);
		}

		/** {@code text} in ink on a paper patch, so it reads over anything. */
		void label(String text, int x, int z) {
			int w = width(text);
			for(int dz = -1; dz <= 5; dz++)
				for(int dx = -1; dx <= w; dx++)
					set(x + dx, z + dz, PAPER);
			text(text, x, z, INK);
		}

		/** {@code text} in the 3 by 5 pixel font, top left at {@code x}, {@code z}. */
		void text(String text, int x, int z, byte color) {
			for(int c = 0; c < text.length(); c++) {
				String[] glyph = glyph(text.charAt(c));
				for(int row = 0; row < 5; row++)
					for(int col = 0; col < 3; col++)
						if(glyph[row].charAt(col) == '#')
							set(x + c * 4 + col, z + row, color);
			}
		}

		static int width(String text) {
			return text.length() * 4 - 1;
		}

		static String[] glyph(char c) {
			String[] extra = EXTRA.get(c);
			return extra != null ? extra : AprsMapPainter.glyph(c);
		}
	}

	/** Punctuation the APRS font lacks. */
	private static final Map<Character, String[]> EXTRA = Map.of(
			' ', new String[] { "...", "...", "...", "...", "..." },
			'.', new String[] { "...", "...", "...", "...", ".#." },
			':', new String[] { "...", ".#.", "...", ".#.", "..." },
			'+', new String[] { "...", ".#.", "###", ".#.", "..." },
			'%', new String[] { "#.#", "..#", ".#.", "#..", "#.#" },
			'>', new String[] { "#..", ".#.", "..#", ".#.", "#.." },
			'<', new String[] { "..#", ".#.", "#..", ".#.", "..#" });

	/** The chart a name like "radar" means, ignoring case, or null. */
	public static Type type(String name) {
		try {
			return Type.valueOf(name.toUpperCase(Locale.ROOT));
		}
		catch(IllegalArgumentException e) {
			return null;
		}
	}
}
