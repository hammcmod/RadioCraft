package com.arrl.radiocraft.common.radio;

import java.util.List;

/**
 * The arithmetic of radio direction finding, apart from the game: compass bearings, how good a bearing is for the
 * number of antenna modules locked on, and the fix where bearings from several stations cross.
 * <p>
 * Bearings are compass degrees, 0 to 360 clockwise from north: north is -Z and east +X, as in Minecraft.
 */
public final class DirectionFinding {

	private DirectionFinding() {}

	/** Fewest modules that must lock before the set gives a bearing at all. */
	public static final int MIN_MODULES = 3;
	/** Most modules one direction finder takes. */
	public static final int MAX_MODULES = 8;
	/** Bearing error (one standard deviation, degrees) by modules locked, from 3 to 8. */
	private static final double[] SIGMA = { 10.0D, 7.0D, 5.0D, 3.5D, 2.5D, 1.5D };
	/** Two stations closer together than this (blocks) see the transmitter from nearly the same place: no fix. */
	public static final double MIN_BASELINE = 48.0D;
	/** Bearings crossing at less than this (degrees) are too near parallel to fix on. */
	public static final double MIN_CROSSING = 15.0D;

	/** Compass bearing from one point to another, 0 up to (not including) 360. */
	public static double bearing(double fromX, double fromZ, double toX, double toZ) {
		double degrees = Math.toDegrees(Math.atan2(toX - fromX, -(toZ - fromZ)));
		return normalize(degrees);
	}

	/** {@code degrees} brought into 0 up to 360. */
	public static double normalize(double degrees) {
		double out = degrees % 360.0D;
		if(out < 0.0D)
			out += 360.0D;
		return out >= 360.0D ? 0.0D : out;
	}

	/** The smallest angle between two bearings, 0 to 180. */
	public static double difference(double a, double b) {
		double d = Math.abs(normalize(a) - normalize(b));
		return d > 180.0D ? 360.0D - d : d;
	}

	/**
	 * How far off (one standard deviation, degrees) a bearing from {@code locked} modules is; infinite below
	 * {@link #MIN_MODULES}. A signal coming down off the ionosphere (skip) arrives from a smeared direction: twice
	 * as bad. A weak one is half as bad again.
	 */
	public static double sigma(int locked, boolean skip, boolean weak) {
		if(locked < MIN_MODULES)
			return Double.POSITIVE_INFINITY;
		double sigma = SIGMA[Math.min(locked, MAX_MODULES) - MIN_MODULES];
		if(skip)
			sigma *= 2.0D;
		if(weak)
			sigma *= 1.5D;
		return sigma;
	}

	/** A bearing taken at a station: where from, which way, and how good (degrees, one standard deviation). */
	public record Line(double x, double z, double bearing, double sigma) {

		/** Unit vector pointing along the bearing. */
		double dirX() {
			return Math.sin(Math.toRadians(bearing));
		}

		double dirZ() {
			return -Math.cos(Math.toRadians(bearing));
		}
	}

	/** Why bearings give no fix, or {@link #OK}. */
	public enum Problem { OK, TOO_FEW, TOO_CLOSE, TOO_PARALLEL, DIVERGE }

	/**
	 * Where the bearings cross, as a best fit (least squares, each weighted by its accuracy), with the half-width of
	 * a box the transmitter is likely inside.
	 */
	public record Fix(double x, double z, double radius, Problem problem) {

		public static Fix none(Problem problem) {
			return new Fix(Double.NaN, Double.NaN, Double.NaN, problem);
		}

		public boolean found() {
			return problem == Problem.OK;
		}
	}

	/**
	 * The fix from two or more bearings. Fails when there are fewer than two, when no two stations are at least
	 * {@link #MIN_BASELINE} apart, when no two bearings cross at {@link #MIN_CROSSING} or more, or when the best
	 * point lies behind one of the stations (the bearings point apart).
	 */
	public static Fix triangulate(List<Line> lines) {
		if(lines.size() < 2)
			return Fix.none(Problem.TOO_FEW);
		boolean farEnough = false, crossing = false;
		double widest = 0.0D;
		for(int i = 0; i < lines.size(); i++)
			for(int j = i + 1; j < lines.size(); j++) {
				Line a = lines.get(i), b = lines.get(j);
				if(Math.hypot(a.x() - b.x(), a.z() - b.z()) >= MIN_BASELINE) {
					farEnough = true;
					double cross = difference(a.bearing(), b.bearing());
					cross = Math.min(cross, 180.0D - cross); // A line crosses another the same way pointing either way.
					widest = Math.max(widest, cross);
					if(cross >= MIN_CROSSING)
						crossing = true;
				}
			}
		if(!farEnough)
			return Fix.none(Problem.TOO_CLOSE);
		if(!crossing)
			return Fix.none(Problem.TOO_PARALLEL);

		// Each line is n . p = n . p0 with n the normal (cos, sin) of the bearing; minimise the weighted squared
		// distances to all lines: (sum w n n^T) p = sum w n (n . p0).
		double a11 = 0.0D, a12 = 0.0D, a22 = 0.0D, b1 = 0.0D, b2 = 0.0D;
		for(Line line : lines) {
			double nx = -line.dirZ(), nz = line.dirX();
			double w = 1.0D / Math.max(0.25D, line.sigma() * line.sigma());
			double c = nx * line.x() + nz * line.z();
			a11 += w * nx * nx;
			a12 += w * nx * nz;
			a22 += w * nz * nz;
			b1 += w * nx * c;
			b2 += w * nz * c;
		}
		double det = a11 * a22 - a12 * a12;
		if(Math.abs(det) < 1.0E-12D)
			return Fix.none(Problem.TOO_PARALLEL);
		double x = (b1 * a22 - b2 * a12) / det;
		double z = (a11 * b2 - a12 * b1) / det;

		double radius = 0.0D;
		for(Line line : lines) {
			double along = (x - line.x()) * line.dirX() + (z - line.z()) * line.dirZ();
			if(along <= 0.0D)
				return Fix.none(Problem.DIVERGE);
			radius = Math.max(radius, along * Math.tan(Math.toRadians(line.sigma())));
		}
		radius /= Math.sin(Math.toRadians(widest)); // Shallow crossings stretch the uncertainty along the lines.
		return new Fix(x, z, Math.max(4.0D, radius), Problem.OK);
	}
}
