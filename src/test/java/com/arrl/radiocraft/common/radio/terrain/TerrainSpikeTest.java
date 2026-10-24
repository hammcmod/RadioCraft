package com.arrl.radiocraft.common.radio.terrain;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spike for terrain-aware propagation (see doc/TerrainPropagationResearch.md). Not wired into the game: it tries the
 * three pieces on synthetic terrain and times them.
 * <ol>
 * <li>{@link Atlas}: a coarse height map, one short per 4 x 4 block cell (16 per chunk), filled from a chunk's
 * heightmap when it loads, so terrain is known for every chunk that has ever been loaded this session.</li>
 * <li>{@link #pathLoss}: knife-edge diffraction over the worst obstacle on the path, from the atlas (ITU-R P.526
 * single edge). Depends on wavelength, so a hill that kills 2m barely dents 80m.</li>
 * <li>{@link #horizon}: a station's horizon elevation in 16 directions, from the atlas, for "how much sky" a skip
 * signal leaving at a given angle sees.</li>
 * </ol>
 */
class TerrainSpikeTest {

    // -------------------- the atlas --------------------

    /** Heights of 4 x 4 block cells, by chunk. Unknown cells (never loaded) read as {@link #UNKNOWN}. */
    static final class Atlas {
        static final int CELL = 4;
        static final short UNKNOWN = Short.MIN_VALUE;
        private final Map<Long, short[]> chunks = new HashMap<>();

        /** What a ChunkEvent.Load would do: the highest motion-blocking block in each 4 x 4 cell of the chunk. */
        void loadChunk(int chunkX, int chunkZ, Terrain terrain) {
            short[] cells = new short[16];
            for(int cx = 0; cx < 4; cx++)
                for(int cz = 0; cz < 4; cz++) {
                    int max = Integer.MIN_VALUE;
                    for(int dx = 0; dx < CELL; dx++)
                        for(int dz = 0; dz < CELL; dz++)
                            max = Math.max(max, terrain.height(chunkX * 16 + cx * CELL + dx, chunkZ * 16 + cz * CELL + dz));
                    cells[cx * 4 + cz] = (short)max;
                }
            chunks.put(key(chunkX, chunkZ), cells);
        }

        int height(int x, int z) {
            short[] cells = chunks.get(key(x >> 4, z >> 4));
            if(cells == null)
                return UNKNOWN;
            return cells[((x & 15) >> 2) * 4 + ((z & 15) >> 2)];
        }

        int chunkCount() {
            return chunks.size();
        }

        private static long key(int chunkX, int chunkZ) {
            return ((long)chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
        }
    }

    interface Terrain {
        int height(int x, int z);
    }

    /** Rolling hills: a few sine waves, 64 to about 140. */
    static final Terrain HILLS = (x, z) -> (int)(64 + 20 * Math.sin(x / 70.0) * Math.cos(z / 90.0) + 12 * Math.sin((x + 2 * z) / 31.0) + 25 * Math.max(0, Math.sin(x / 400.0)));

    // -------------------- path loss --------------------

    /** ITU-R P.526 knife-edge loss in dB for the diffraction parameter v (0 for v below -0.78). */
    static double knifeEdgeDb(double v) {
        if(v <= -0.78D)
            return 0.0D;
        double a = v - 0.1D;
        return 6.9D + 20.0D * Math.log10(Math.sqrt(a * a + 1.0D) + a);
    }

    /**
     * Share of the line-of-sight strength left after the worst obstacle between two antennas, as a field-strength
     * factor (dB / 20). Samples the atlas every cell along the path, skipping a cell at each end (the station's own
     * roof and mast). Unknown cells count as clear.
     */
    static double pathLoss(Atlas atlas, double ax, double ay, double az, double bx, double by, double bz, double wavelength) {
        double dx = bx - ax, dz = bz - az;
        double distance = Math.sqrt(dx * dx + dz * dz);
        int steps = (int)(distance / Atlas.CELL);
        double worstV = -10.0D;
        for(int i = 2; i < steps - 1; i++) {
            double f = (double)i / steps;
            int h = atlas.height((int)Math.floor(ax + dx * f), (int)Math.floor(az + dz * f));
            if(h == Atlas.UNKNOWN)
                continue;
            double d1 = distance * f, d2 = distance - d1;
            double clearance = h - (ay + (by - ay) * f); // Positive: the ground pokes above the straight line.
            double v = clearance * Math.sqrt(2.0D * distance / (wavelength * d1 * d2));
            worstV = Math.max(worstV, v);
        }
        return Math.pow(10.0D, -knifeEdgeDb(worstV) / 20.0D);
    }

    // -------------------- horizon --------------------

    static final int AZIMUTHS = 16;

    /** The horizon elevation (radians) in 16 directions around a station, from terrain within {@code radius} blocks. */
    static float[] horizon(Atlas atlas, double x, double y, double z, int radius) {
        float[] angles = new float[AZIMUTHS];
        for(int a = 0; a < AZIMUTHS; a++) {
            double bearing = a * 2.0D * Math.PI / AZIMUTHS;
            double sx = Math.sin(bearing), sz = Math.cos(bearing);
            double best = 0.0D;
            for(int r = Atlas.CELL * 2; r <= radius; r += Atlas.CELL) { // Skip the station's own cell.
                int h = atlas.height((int)Math.floor(x + sx * r), (int)Math.floor(z + sz * r));
                if(h != Atlas.UNKNOWN)
                    best = Math.max(best, Math.atan2(h - y, r));
            }
            angles[a] = (float)best;
        }
        return angles;
    }

    /**
     * Share of a skip signal leaving at {@code takeoff} radians towards {@code bearing} that clears the horizon: 1 if
     * the horizon there is below the takeoff angle, fading to 0 when it is 10 degrees above it.
     */
    static double skipClearance(float[] horizon, double bearing, double takeoff) {
        int a = (int)Math.round(bearing / (2.0D * Math.PI) * AZIMUTHS) & (AZIMUTHS - 1);
        double excess = horizon[a] - takeoff;
        return excess <= 0.0D ? 1.0D : Math.max(0.0D, 1.0D - excess / Math.toRadians(10.0D));
    }

    // -------------------- checks --------------------

    @Test
    void knifeEdgeMatchesTheTextbook() {
        assertEquals(0.0D, knifeEdgeDb(-1.0D), 1e-9);
        assertEquals(6.0D, knifeEdgeDb(0.0D), 0.1D); // Grazing: 6 dB.
        assertTrue(knifeEdgeDb(2.4D) > 19.5D && knifeEdgeDb(2.4D) < 21.0D, "v=2.4: " + knifeEdgeDb(2.4D));
    }

    @Test
    void aHillBlocksVhfMoreThanHf() {
        Atlas atlas = new Atlas();
        Terrain ridge = (x, z) -> (x >= 240 && x < 260) ? 100 : 64; // A 36 block ridge halfway along.
        for(int cx = -2; cx < 34; cx++)
            for(int cz = -2; cz < 3; cz++)
                atlas.loadChunk(cx, cz, ridge);
        double vhf = pathLoss(atlas, 0, 66, 8, 500, 66, 8, 2.0);
        double hf80 = pathLoss(atlas, 0, 66, 8, 500, 66, 8, 80.0);
        double flat = pathLoss(atlas, 0, 120, 8, 500, 120, 8, 2.0); // Same path with both antennas above the ridge.
        assertTrue(vhf < 0.1D, "2m over a 36 block ridge: " + vhf);
        assertTrue(hf80 > vhf * 3.0D, "80m should diffract round it: 80m " + hf80 + " vs 2m " + vhf);
        assertTrue(flat > 0.9D, "clear path: " + flat);
    }

    @Test
    void horizonSeesTheValleyWalls() {
        Atlas atlas = new Atlas();
        Terrain valley = (x, z) -> Math.abs(x) < 20 ? 64 : 120; // A deep valley running north-south.
        for(int cx = -6; cx < 6; cx++)
            for(int cz = -6; cz < 6; cz++)
                atlas.loadChunk(cx, cz, valley);
        float[] h = horizon(atlas, 0.5, 66, 0.5, 64);
        assertTrue(h[4] > Math.toRadians(30), "east wall: " + Math.toDegrees(h[4]));   // Bearing 90 degrees.
        assertTrue(h[0] < Math.toRadians(1), "along the valley: " + Math.toDegrees(h[0]));
        assertEquals(0.0D, skipClearance(h, Math.PI / 2, Math.toRadians(10)), 1e-9, "low-angle 10m skip east is blocked");
        assertEquals(1.0D, skipClearance(h, 0.0D, Math.toRadians(10)), 1e-9, "north along the valley is clear");
        assertEquals(1.0D, skipClearance(h, Math.PI / 2, Math.toRadians(70)), 1e-9, "80m NVIS goes straight up: clear");
    }

    /** Times the pieces on rolling hills; prints the numbers (no thresholds: machines differ). */
    @Test
    void timing() {
        Atlas atlas = new Atlas();
        long t0 = System.nanoTime();
        for(int cx = -64; cx < 64; cx++)
            for(int cz = -64; cz < 64; cz++)
                atlas.loadChunk(cx, cz, HILLS);
        long fill = System.nanoTime() - t0;

        Random random = new Random(1);
        int paths = 200_000;
        double sink = 0.0D;
        t0 = System.nanoTime();
        for(int i = 0; i < paths; i++) {
            double ax = random.nextInt(1600) - 800, az = random.nextInt(1600) - 800;
            double angle = random.nextDouble() * 2 * Math.PI, length = 100 + random.nextInt(400);
            double bx = ax + Math.sin(angle) * length, bz = az + Math.cos(angle) * length;
            sink += pathLoss(atlas, ax, HILLS.height((int)ax, (int)az) + 2, az, bx, HILLS.height((int)bx, (int)bz) + 2, bz, 2.0);
        }
        long path = System.nanoTime() - t0;

        int stations = 20_000;
        t0 = System.nanoTime();
        for(int i = 0; i < stations; i++) {
            double x = random.nextInt(1600) - 800, z = random.nextInt(1600) - 800;
            sink += horizon(atlas, x, HILLS.height((int)x, (int)z) + 2, z, 64)[0];
        }
        long horizon = System.nanoTime() - t0;

        System.out.printf("[terrain spike] atlas: %d chunks filled in %.1f ms (%.2f us/chunk), %d KB%n",
                atlas.chunkCount(), fill / 1e6, fill / 1e3 / atlas.chunkCount(), atlas.chunkCount() * (16 * 2 + 48) / 1024);
        System.out.printf("[terrain spike] path loss, 100-500 blocks: %.2f us/path%n", path / 1e3 / paths);
        System.out.printf("[terrain spike] horizon, 16 directions x 64 blocks: %.2f us/station (sink %.1f)%n", horizon / 1e3 / stations, sink);
        assertTrue(sink > 0);
    }
}
