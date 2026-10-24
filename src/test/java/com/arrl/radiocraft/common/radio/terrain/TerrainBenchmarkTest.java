package com.arrl.radiocraft.common.radio.terrain;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What terrain costs, timed on the real atlas, maths and cache over synthetic rolling hills. Prints its numbers (run
 * with {@code ./gradlew test --tests '*TerrainBenchmarkTest' -i}); there are no thresholds, machines differ.
 * <p>
 * The busy-server simulation plays ten seconds of voice: 10 stations talking at once (5 fixed HF stations, 5 walking
 * handhelds), each voice frame (50 a second) going to 40 receivers (half of them walking handhelds), as
 * {@link TerrainPropagation} does it: line-of-sight loss per pair of 4 block cells and a horizon per cell, both kept
 * for the recalculation time. It runs once working everything out for every packet and once with the 2 second
 * default, and reports the voice thread time per second of talk.
 */
class TerrainBenchmarkTest {

    /** Rolling hills: a few sine waves, 64 to about 140. */
    static final TerrainMath.HeightSource HILLS = (x, z) -> (int)(64 + 20 * Math.sin(x / 70.0) * Math.cos(z / 90.0)
            + 12 * Math.sin((x + 2 * z) / 31.0) + 25 * Math.max(0, Math.sin(x / 400.0)));

    private static final double VHF = 2.08D, HF20 = 20.9D;

    static TerrainAtlas hillAtlas(int radiusChunks) {
        TerrainAtlas atlas = new TerrainAtlas();
        for(int cx = -radiusChunks; cx < radiusChunks; cx++)
            for(int cz = -radiusChunks; cz < radiusChunks; cz++)
                atlas.put(cx, cz, sample(cx, cz), Integer.MAX_VALUE);
        return atlas;
    }

    /** What {@link TerrainAtlas#sample} does to a real chunk, over the synthetic hills. */
    static short[] sample(int chunkX, int chunkZ) {
        short[] cells = new short[16];
        for(int cx = 0; cx < 4; cx++)
            for(int cz = 0; cz < 4; cz++) {
                int sum = 0;
                for(int dx = 0; dx < 4; dx++)
                    for(int dz = 0; dz < 4; dz++)
                        sum += HILLS.surface(chunkX * 16 + cx * 4 + dx, chunkZ * 16 + cz * 4 + dz);
                cells[cx * 4 + cz] = (short)Math.round(sum / 16.0F);
            }
        return cells;
    }

    @Test
    void pieces() {
        long t0 = System.nanoTime();
        TerrainAtlas atlas = hillAtlas(64);
        long fill = System.nanoTime() - t0;

        Random random = new Random(1);
        int paths = 200_000;
        double sink = 0.0D;
        t0 = System.nanoTime();
        for(int i = 0; i < paths; i++) {
            double ax = random.nextInt(1600) - 800, az = random.nextInt(1600) - 800;
            double angle = random.nextDouble() * 2 * Math.PI, length = 100 + random.nextInt(400);
            double bx = ax + Math.sin(angle) * length, bz = az + Math.cos(angle) * length;
            sink += TerrainMath.lineOfSightLossDb(atlas, ax, HILLS.surface((int)ax, (int)az) + 1.5, az, bx, HILLS.surface((int)bx, (int)bz) + 1.5, bz,
                    VHF, 8, 0, 18);
        }
        long path = System.nanoTime() - t0;

        int stations = 20_000;
        t0 = System.nanoTime();
        for(int i = 0; i < stations; i++) {
            double x = random.nextInt(1600) - 800, z = random.nextInt(1600) - 800;
            sink += TerrainMath.horizon(atlas, x, HILLS.surface((int)x, (int)z) + 1.5, z, TerrainMath.HORIZON_RADIUS)[0];
        }
        long horizon = System.nanoTime() - t0;

        System.out.printf("[terrain] atlas fill: %d chunks in %.1f ms (%.2f us/chunk, synthetic heights; a real chunk reads its heightmap)%n",
                atlas.chunkCount(), fill / 1e6, fill / 1e3 / atlas.chunkCount());
        System.out.printf("[terrain] line-of-sight loss, 100-500 blocks, uncached: %.2f us/path%n", path / 1e3 / paths);
        System.out.printf("[terrain] horizon, 16 directions x 64 blocks, uncached: %.2f us/station (sink %.1f)%n", horizon / 1e3 / stations, sink);
        assertTrue(sink != 0);
    }

    @Test
    void memory() {
        Runtime rt = Runtime.getRuntime();
        System.gc();
        long before = rt.totalMemory() - rt.freeMemory();
        TerrainAtlas atlas = new TerrainAtlas();
        for(int i = 0; i < 100_000; i++)
            atlas.put(i % 317, i / 317, new short[16], Integer.MAX_VALUE);
        System.gc();
        long after = rt.totalMemory() - rt.freeMemory();
        System.out.printf("[terrain] atlas memory: %d chunks take about %.1f MB (%d bytes/chunk measured, %d estimated)%n",
                atlas.chunkCount(), (after - before) / 1048576.0, (after - before) / atlas.chunkCount(), TerrainAtlas.BYTES_PER_CHUNK);
        assertTrue(atlas.chunkCount() == 100_000);
    }

    private record PathKey(long a, long b) {}

    private static long cell(double x, double y, double z) {
        return (((long)Math.floor(x) >> 2) & 0x3FFFFFL) << 42 | (((long)Math.floor(y) >> 2) & 0xFFFFFL) << 22 | (((long)Math.floor(z) >> 2) & 0x3FFFFFL);
    }

    /** Voice thread time per second of talk on a busy server, recalculating every packet or every 2 seconds. */
    @Test
    void busyServer() {
        TerrainAtlas atlas = hillAtlas(48);
        double everyPacket = simulate(atlas, 0L);
        double cached = simulate(atlas, 2_000_000_000L);
        System.out.printf("[terrain] busy server (10 talking, 40 listening, 50 frames/s): every packet %.2f ms per second of talk, cached 2 s %.3f ms per second of talk%n",
                everyPacket, cached);
        System.out.printf("[terrain] that is %.2f%% and %.3f%% of one core%n", everyPacket / 10.0, cached / 10.0);
        assertTrue(everyPacket > 0);
    }

    /** Milliseconds of terrain work per simulated second. */
    private static double simulate(TerrainAtlas atlas, long ttl) {
        Random random = new Random(7);
        int talkers = 10, listeners = 40, seconds = 10, frames = 50;
        double[][] talk = new double[talkers][], listen = new double[listeners][];
        for(int i = 0; i < talkers; i++)
            talk[i] = new double[]{random.nextInt(800) - 400, random.nextInt(800) - 400, random.nextDouble() * 2 * Math.PI};
        for(int i = 0; i < listeners; i++)
            listen[i] = new double[]{random.nextInt(800) - 400, random.nextInt(800) - 400, random.nextDouble() * 2 * Math.PI};
        ExpiringCache<PathKey, Double> paths = new ExpiringCache<>(50_000);
        ExpiringCache<Long, float[]> horizons = new ExpiringCache<>(50_000);
        double sink = 0.0D;
        long work = 0L;
        for(int f = 0; f < seconds * frames; f++) {
            long now = f * 20_000_000L; // Simulated time: 20 ms a frame.
            for(int i = 5; i < talkers; i++) walk(talk[i], random); // Talkers 5-9 and odd listeners are walking handhelds.
            for(int i = 1; i < listeners; i += 2) walk(listen[i], random);
            long t0 = System.nanoTime();
            for(int t = 0; t < talkers; t++) {
                double tx = talk[t][0], tz = talk[t][1], ty = HILLS.surface((int)tx, (int)tz) + 1.5;
                boolean hf = t < 5;
                for(double[] l : listen) {
                    double lx = l[0], lz = l[1], ly = HILLS.surface((int)lx, (int)lz) + 1.5;
                    double dx = lx - tx, dz = lz - tz;
                    if(dx * dx + dz * dz < (hf ? 300 * 300 : 500 * 500)) { // Line of sight.
                        long a = cell(tx, ty, tz), b = cell(lx, ly, lz);
                        double wl = hf ? HF20 : VHF;
                        sink += paths.get(new PathKey(Math.min(a, b), Math.max(a, b)), now, ttl,
                                k -> TerrainMath.lineOfSightLossDb(atlas, tx, ty, tz, lx, ly, lz, wl, 8, hf ? 4 : 0, 18));
                    } else if(hf) { // Skip: both ends' horizons.
                        sink += TerrainMath.skipClearance(horizons.get(cell(tx, ty, tz), now, ttl, k -> TerrainMath.horizon(atlas, tx, ty, tz, 64)),
                                TerrainMath.bearing(tx, tz, lx, lz), Math.toRadians(10), Math.toRadians(10));
                        sink += TerrainMath.skipClearance(horizons.get(cell(lx, ly, lz), now, ttl, k -> TerrainMath.horizon(atlas, lx, ly, lz, 64)),
                                TerrainMath.bearing(lx, lz, tx, tz), Math.toRadians(10), Math.toRadians(10));
                    }
                }
            }
            work += System.nanoTime() - t0;
        }
        assertTrue(!Double.isNaN(sink));
        return work / 1e6 / seconds;
    }

    /** A player walking at 4 blocks a second, turning now and then. */
    private static void walk(double[] p, Random random) {
        if(random.nextInt(100) == 0)
            p[2] = random.nextDouble() * 2 * Math.PI;
        p[0] += Math.sin(p[2]) * 0.08;
        p[1] += Math.cos(p[2]) * 0.08;
    }
}
