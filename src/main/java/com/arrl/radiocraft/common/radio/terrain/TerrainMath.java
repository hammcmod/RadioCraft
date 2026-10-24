package com.arrl.radiocraft.common.radio.terrain;

/**
 * The maths of terrain-aware propagation, free of the game so it can be unit tested and timed (see
 * doc/TerrainPropagationResearch.md). Heights come from a {@link HeightSource}: in game the {@link TerrainAtlas}, one
 * surface height per 4 x 4 block cell.
 * <ul>
 * <li>{@link #lineOfSightLossDb}: knife-edge diffraction over the worst obstacle on the path (ITU-R P.526 single
 * edge), which depends on wavelength, so a hill that kills 2m only dents 80m; plus a loss per cliff for HF ground
 * wave.</li>
 * <li>{@link #horizon} and {@link #skipClearance}: a station's horizon in 16 directions, and how much of a skip
 * signal leaving at a band's takeoff angle clears it.</li>
 * </ul>
 */
public final class TerrainMath {

    /** Width of an atlas cell, in blocks. */
    public static final int CELL = 4;
    /** What a {@link HeightSource} returns for terrain nobody has loaded this session: counted as clear. */
    public static final int UNKNOWN = Integer.MIN_VALUE;
    /** Blocks at each end of a path (the station's own roof, mast and yard) that do not count as obstacles. */
    public static final int END_CLEARANCE = 8;
    /** Directions in a horizon profile. */
    public static final int AZIMUTHS = 16;
    /** How far out a horizon profile looks, in blocks. */
    public static final int HORIZON_RADIUS = 64;

    private static final double GRAZING_DB = knifeEdgeDb(0.0D);

    private TerrainMath() {}

    /** Surface height (top of the highest solid block) around a block column, or {@link #UNKNOWN}. */
    public interface HeightSource {
        int surface(int x, int z);
    }

    /** ITU-R P.526 knife-edge loss in dB for the diffraction parameter v (0 below v = -0.78, 6 dB at grazing). */
    public static double knifeEdgeDb(double v) {
        if(v <= -0.78D)
            return 0.0D;
        double a = v - 0.1D;
        return 6.9D + 20.0D * Math.log10(Math.sqrt(a * a + 1.0D) + a);
    }

    /**
     * Loss an obstacle adds over what the game already gives a clear path: nothing until the ground reaches the
     * straight line between the antennas (v = 0), then the knife-edge loss beyond grazing. Flat ground and valleys
     * cost nothing; anything poking into the line costs, more for short wavelengths.
     */
    public static double obstructionDb(double v) {
        return v <= 0.0D ? 0.0D : knifeEdgeDb(v) - GRAZING_DB;
    }

    /**
     * Terrain loss in dB on the straight path between two antennas (positions in blocks, heights are the antennas'
     * own): the worse of the knife edge over the worst obstacle and the cliffs crossed. Samples every cell along the
     * path, skipping {@link #END_CLEARANCE} blocks at each end; unknown cells count as clear.
     *
     * @param wavelength the signal's wavelength in blocks (1 block = 1 m)
     * @param cliffRise  height step between neighbouring samples (4 blocks apart) that counts as a cliff
     * @param cliffDb    loss per cliff crossed (0 for none: VHF has the knife edge alone)
     * @param cliffCapDb most the cliffs can cost together
     */
    public static double lineOfSightLossDb(HeightSource heights, double ax, double ay, double az, double bx, double by, double bz,
                                           double wavelength, double cliffRise, double cliffDb, double cliffCapDb) {
        double dx = bx - ax, dz = bz - az;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if(distance <= 2 * END_CLEARANCE)
            return 0.0D;
        int steps = (int)(distance / CELL);
        double worstV = Double.NEGATIVE_INFINITY;
        double cliffs = 0.0D;
        int last = UNKNOWN;
        for(int i = 0; i <= steps; i++) {
            double d1 = distance * i / steps;
            if(d1 < END_CLEARANCE || d1 > distance - END_CLEARANCE)
                continue;
            double f = d1 / distance;
            int h = heights.surface((int)Math.floor(ax + dx * f), (int)Math.floor(az + dz * f));
            if(h == UNKNOWN) {
                last = UNKNOWN;
                continue;
            }
            double d2 = distance - d1;
            double line = ay + (by - ay) * f;
            double scale = Math.sqrt(2.0D * distance / (wavelength * d1 * d2));
            double v = (h - line) * scale; // Positive: the ground pokes above the straight line.
            worstV = Math.max(worstV, v);
            // A cliff counts when its top reaches into the first Fresnel zone (v > -sqrt 2): ground far below the
            // path, under a mast or a mountain-top station, does not disturb it.
            if(cliffDb > 0.0D && last != UNKNOWN && Math.abs(h - last) >= cliffRise && (Math.max(h, last) - line) * scale > -Math.sqrt(2.0D))
                cliffs += cliffDb;
            last = h;
        }
        return Math.max(obstructionDb(worstV), Math.min(cliffs, cliffCapDb)); // Whichever is worse: one wall is not both.
    }

    /**
     * The horizon elevation (radians, 0 or more) in {@link #AZIMUTHS} directions around a point, from terrain between
     * {@link #END_CLEARANCE} and {@code radius} blocks out. Direction {@code a} is bearing {@code a * 2 pi / AZIMUTHS},
     * bearing 0 being +Z and pi / 2 being +X (see {@link #bearing}).
     */
    public static float[] horizon(HeightSource heights, double x, double y, double z, int radius) {
        float[] angles = new float[AZIMUTHS];
        for(int a = 0; a < AZIMUTHS; a++) {
            double bearing = a * 2.0D * Math.PI / AZIMUTHS;
            double sx = Math.sin(bearing), sz = Math.cos(bearing);
            double best = 0.0D;
            for(int r = END_CLEARANCE; r <= radius; r += CELL) {
                int h = heights.surface((int)Math.floor(x + sx * r), (int)Math.floor(z + sz * r));
                if(h != UNKNOWN)
                    best = Math.max(best, Math.atan2(h - y, r));
            }
            angles[a] = (float)best;
        }
        return angles;
    }

    /** Bearing from one point to another in the horizontal plane, in the convention {@link #horizon} uses. */
    public static double bearing(double fromX, double fromZ, double toX, double toZ) {
        return Math.atan2(toX - fromX, toZ - fromZ);
    }

    /**
     * Share of a skip signal leaving at {@code takeoff} radians towards {@code bearing} that clears the horizon: 1 if
     * the horizon there is below the takeoff angle, fading to 0 when it is {@code fade} radians above it.
     */
    public static double skipClearance(float[] horizon, double bearing, double takeoff, double fade) {
        int a = (int)Math.round(bearing / (2.0D * Math.PI) * AZIMUTHS) & (AZIMUTHS - 1);
        double excess = horizon[a] - takeoff;
        return excess <= 0.0D ? 1.0D : Math.max(0.0D, 1.0D - excess / fade);
    }

    /** A loss in dB as a share of the signal's strength (strengths add as power, like antenna gains in dBi). */
    public static double dbToFactor(double db) {
        return db <= 0.0D ? 1.0D : Math.pow(10.0D, -db / 10.0D);
    }
}
