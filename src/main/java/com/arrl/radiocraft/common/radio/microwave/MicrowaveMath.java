package com.arrl.radiocraft.common.radio.microwave;

import com.arrl.radiocraft.common.radio.DirectionFinding;
import com.arrl.radiocraft.common.radio.terrain.TerrainMath;

/**
 * The geometry of a microwave link, apart from the game, modelled on lining up a real point to point link. Two dishes
 * hear something of each other as soon as each points within {@link #VISIBLE_ANGLE} of the other, the straight line
 * between them clears the terrain, and they are within range. Range is the geometric mean of the two ends' ranges (a
 * dish and a horn meet in between), less any blackout loss.
 * <p>
 * How well they hear each other is the received signal level ({@link Signal#rssi}, dBm): strongest close in and dead
 * on, weaker with distance and with the pointing loss at each end, which follows a dish's main lobe ({@link
 * #pointingLoss}: 3 dB at half the beam width off axis, 12 dB at the full width) down to its sidelobes ({@link
 * #SIDELOBE_LOSS}). Rough aiming shows a weak reading; peaking one end, then the other, brings it past
 * {@link #SENSITIVITY_DBM}, where the modems decode each other and the link can come up, and on to {@link #GOOD_DBM}
 * for clean calls. A short path is forgiving: two dishes a few dozen blocks apart link even when roughly aimed.
 * <p>
 * Positions are block coordinates; azimuths are compass degrees (0 north, -Z; 90 east, +X).
 */
public final class MicrowaveMath {

	private MicrowaveMath() {}

	/** The lowest and highest frequency a microwave radio tunes, MHz (the 6 GHz band), and its step. */
	public static final int MIN_MHZ = 5925, MAX_MHZ = 7125, STEP_MHZ = 10;
	/** Blocks of the path left out of the terrain check at each end: the dish's own mast and roof. */
	public static final double END_CLEARANCE = 4.0D;
	/** How far below the straight line the ground must stay, blocks. */
	public static final double CLEARANCE = 1.0D;

	/** Lowest and highest a dish can point, degrees above the horizon. */
	public static final double MIN_ELEVATION = -45.0D, MAX_ELEVATION = 90.0D;

	/**
	 * One end: where the dish is, where it points (compass azimuth, and elevation in degrees above the horizon), and
	 * how far it reaches on its own.
	 */
	public record End(double x, double y, double z, double azimuth, double elevation, double range) {

		/** Unit vector along where the dish points. */
		double[] pointing() {
			double az = Math.toRadians(azimuth), el = Math.toRadians(elevation);
			return new double[] { Math.sin(az) * Math.cos(el), Math.sin(el), -Math.cos(az) * Math.cos(el) };
		}
	}

	/** The range two ends reach together. */
	public static double linkRange(double rangeA, double rangeB, double loss) {
		return Math.max(0.0D, Math.sqrt(rangeA * rangeB) - loss);
	}

	/** The angle in degrees between where the dish at {@code from} points and the straight line to the point. */
	public static double offAxis(End from, double toX, double toY, double toZ) {
		double dx = toX - from.x(), dy = toY - from.y(), dz = toZ - from.z();
		double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if(length < 1.0E-6D)
			return 0.0D;
		double[] p = from.pointing();
		double cos = (p[0] * dx + p[1] * dy + p[2] * dz) / length;
		return Math.toDegrees(Math.acos(Math.max(-1.0D, Math.min(1.0D, cos))));
	}

	/** True if a dish at {@code from} sees the point inside its beam (up and down as well as round). */
	public static boolean inBeam(End from, double toX, double toY, double toZ, double beamWidth) {
		return offAxis(from, toX, toY, toZ) <= beamWidth;
	}

	/** The azimuth and elevation that point a dish at {@code from} straight at the point. */
	public static double[] aimAt(End from, double toX, double toY, double toZ) {
		double dx = toX - from.x(), dy = toY - from.y(), dz = toZ - from.z();
		return new double[] { DirectionFinding.bearing(from.x(), from.z(), toX, toZ), Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))) };
	}

	/**
	 * True if the ground stays at least {@link #CLEARANCE} below the straight line from {@code a} to {@code b} along the
	 * whole path, ends excepted. Unknown ground (an unexplored chunk) counts as clear.
	 */
	public static boolean clearPath(TerrainMath.HeightSource heights, End a, End b) {
		if(heights == null)
			return true;
		double dx = b.x() - a.x(), dz = b.z() - a.z();
		double distance = Math.sqrt(dx * dx + dz * dz);
		if(distance <= 2.0D * END_CLEARANCE)
			return true;
		int steps = (int)Math.ceil(distance / 2.0D);
		for(int i = 0; i <= steps; i++) {
			double d = distance * i / steps;
			if(d < END_CLEARANCE || d > distance - END_CLEARANCE)
				continue;
			double f = d / distance;
			int h = heights.surface((int)Math.floor(a.x() + dx * f), (int)Math.floor(a.z() + dz * f));
			if(h == TerrainMath.UNKNOWN)
				continue;
			double line = a.y() + (b.y() - a.y()) * f;
			if(h > line - CLEARANCE)
				return false;
		}
		return true;
	}

	/** Weakest signal the modems decode, dBm: the far end's callsign shows and a link can come up. */
	public static final double SENSITIVITY_DBM = -90.0D;
	/** An established link holds down to this, a little below {@link #SENSITIVITY_DBM}, so it does not flap. */
	public static final double HOLD_DBM = -93.0D;
	/** Below this a link works but is marginal (telephone calls break up), dBm. */
	public static final double GOOD_DBM = -75.0D;
	/** Between {@link #GOOD_DBM} and this the meter reads FAIR; below it, down to {@link #SENSITIVITY_DBM}, POOR. */
	public static final double FAIR_DBM = -82.0D;
	/**
	 * Past the edge of range the signal keeps falling instead of vanishing: from {@link #EDGE_DBM} at the range to
	 * {@link #FLOOR_DBM} at this many times it, so a dish just out of range aimed right still shows a weak bar.
	 */
	public static final double OUT_OF_RANGE_FACTOR = 1.25D;
	/** The signal from right beside the far dish, dead on, and from the edge of range, dBm. */
	public static final double NEAR_DBM = -40.0D, EDGE_DBM = -70.0D;
	/** The most a pointing error costs at one end: what a dish still picks up on its sidelobes, dB. */
	public static final double SIDELOBE_LOSS = 30.0D;
	/** Degrees off axis inside which a dish hears anything of the far end at all. */
	public static final double VISIBLE_ANGLE = 90.0D;
	/** The weakest reading a meter shows for a signal that is there at all, just above its noise, dBm. */
	public static final double FLOOR_DBM = -118.0D;
	/** What the screens show for no signal at all. */
	public static final double NO_SIGNAL_DBM = -120.0D;

	/**
	 * dB lost with a dish {@code offAxis} degrees off the line, for a dish of {@code beamWidth} degrees (its half power
	 * width): a main lobe {@code 12 (off / width)^2} deep, so 3 dB at half the width either side and 12 at the full width,
	 * bottoming out on the sidelobes at {@link #SIDELOBE_LOSS}.
	 */
	public static double pointingLoss(double offAxis, double beamWidth) {
		double f = offAxis / Math.max(1.0E-6D, beamWidth);
		return Math.min(SIDELOBE_LOSS, 12.0D * f * f);
	}

	/**
	 * The signal dead on at {@code distance} of a link reaching {@code range}: {@link #NEAR_DBM} close in to
	 * {@link #EDGE_DBM} at the edge, then straight down to {@link #FLOOR_DBM} at {@link #OUT_OF_RANGE_FACTOR} times the range.
	 */
	public static double distanceLevel(double distance, double range) {
		range = Math.max(1.0D, range);
		if(distance > range)
			return EDGE_DBM + (FLOOR_DBM - EDGE_DBM) * Math.min(1.0D, (distance / range - 1.0D) / (OUT_OF_RANGE_FACTOR - 1.0D));
		return NEAR_DBM + (EDGE_DBM - NEAR_DBM) * Math.log10(1.0D + 9.0D * Math.max(0.0D, distance) / range);
	}

	/** What the meter calls a level: the only thing the radio knows about the far end. */
	public enum Grade {
		NONE("NO SIGNAL"), WEAK("TOO WEAK"), POOR("POOR"), FAIR("FAIR"), GOOD("GOOD");

		public final String label;

		Grade(String label) {
			this.label = label;
		}
	}

	public static Grade grade(double rssi) {
		if(rssi <= NO_SIGNAL_DBM + 0.5D)
			return Grade.NONE;
		if(rssi < SENSITIVITY_DBM)
			return Grade.WEAK;
		if(rssi < FAIR_DBM)
			return Grade.POOR;
		return rssi < GOOD_DBM ? Grade.FAIR : Grade.GOOD;
	}

	/** Why two dishes do or do not link. */
	public enum Result { OK, OUT_OF_RANGE, NOT_FACING, BLOCKED, WEAK }

	/** A link's state and its signal level (dBm, {@link #NO_SIGNAL_DBM} when there is none). */
	public record Signal(Result result, double rssi) {}

	/** Whether the straight path between two ends is clear: {@link MicrowavePaths} in a level, {@link #clearPath} on the atlas. */
	@FunctionalInterface
	public interface Path {
		boolean clear(End a, End b);
	}

	/** {@link #signalAlong} with the path judged on the terrain atlas ({@link #clearPath}). */
	public static Signal signal(TerrainMath.HeightSource heights, End a, End b, double beamWidth, double loss) {
		return signalAlong((x, y) -> clearPath(heights, x, y), a, b, beamWidth, loss);
	}

	/**
	 * How two dishes hear each other: in range, each within {@link #VISIBLE_ANGLE} of the other, a clear path, and the
	 * signal level for dishes {@code beamWidth} degrees wide. A signal that is there at all reads at least
	 * {@link #FLOOR_DBM}, so a roughly aimed pair always shows something to peak.
	 */
	public static Signal signalAlong(Path path, End a, End b, double beamWidth, double loss) {
		double distance = Math.hypot(b.x() - a.x(), b.z() - a.z());
		double range = linkRange(a.range(), b.range(), loss);
		if(distance > range * OUT_OF_RANGE_FACTOR)
			return new Signal(Result.OUT_OF_RANGE, NO_SIGNAL_DBM);
		double offA = offAxis(a, b.x(), b.y(), b.z()), offB = offAxis(b, a.x(), a.y(), a.z());
		if(offA > VISIBLE_ANGLE || offB > VISIBLE_ANGLE)
			return new Signal(Result.NOT_FACING, NO_SIGNAL_DBM);
		if(!path.clear(a, b))
			return new Signal(Result.BLOCKED, NO_SIGNAL_DBM);
		double rssi = Math.max(FLOOR_DBM, distanceLevel(distance, range) - pointingLoss(offA, beamWidth) - pointingLoss(offB, beamWidth));
		return new Signal(rssi >= SENSITIVITY_DBM ? Result.OK : Result.WEAK, rssi);
	}

	/** Whether two dishes link: in range, facing each other, with a clear path, and loud enough. */
	public static Result check(TerrainMath.HeightSource heights, End a, End b, double beamWidth, double loss) {
		return signal(heights, a, b, beamWidth, loss).result();
	}

	/** True if two frequencies (MHz) are close enough to spoil each other's link. */
	public static boolean interferes(int mhzA, int mhzB, int window) {
		return Math.abs(mhzA - mhzB) <= window;
	}

	/** {@code mhz} moved {@code steps} steps, kept in the band. */
	public static int tune(int mhz, int steps) {
		int stepped = mhz + steps * STEP_MHZ;
		return Math.max(MIN_MHZ, Math.min(MAX_MHZ, stepped));
	}

	/** The ID as four upper case hex digits. */
	public static String hex(int id) {
		return String.format("%04X", id & 0xFFFF);
	}
}
