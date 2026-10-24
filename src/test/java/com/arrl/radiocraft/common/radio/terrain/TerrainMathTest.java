package com.arrl.radiocraft.common.radio.terrain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainMathTest {

    /** A height source straight from a formula (no atlas). */
    static TerrainMath.HeightSource ground(TerrainMath.HeightSource f) {
        return f;
    }

    /** 2m and 80m wavelengths in blocks. */
    private static final double VHF = 2.08D, HF80 = 85.7D, HF10 = 10.7D;

    private static double los(TerrainMath.HeightSource heights, double wavelength, double cliffDb) {
        return TerrainMath.lineOfSightLossDb(heights, 0.5, 66.5, 0.5, 500.5, 66.5, 0.5, wavelength, 8, cliffDb, 18);
    }

    @Test
    void knifeEdgeMatchesTheTextbook() {
        assertEquals(0.0D, TerrainMath.knifeEdgeDb(-1.0D), 1e-9);
        assertEquals(6.0D, TerrainMath.knifeEdgeDb(0.0D), 0.1D);
        assertTrue(TerrainMath.knifeEdgeDb(2.4D) > 19.5D && TerrainMath.knifeEdgeDb(2.4D) < 21.0D);
    }

    @Test
    void flatGroundCostsNothing() {
        assertEquals(0.0D, los(ground((x, z) -> 65), VHF, 0), 1e-9);
    }

    @Test
    void aRidgeKillsVhfAndDentsHf() {
        TerrainMath.HeightSource ridge = (x, z) -> (x >= 240 && x < 260) ? 101 : 65; // 36 blocks high, halfway.
        double vhf = TerrainMath.dbToFactor(los(ridge, VHF, 0));
        double hf = TerrainMath.dbToFactor(los(ridge, HF80, 0));
        assertTrue(vhf < 0.05D, "2m over the ridge keeps " + vhf);
        assertTrue(hf > 0.3D, "80m round the ridge keeps " + hf);
    }

    @Test
    void cliffsCostHfGroundWave() {
        TerrainMath.HeightSource gentle = (x, z) -> 65 - x / 100;            // 5 blocks down over 500: no cliff.
        TerrainMath.HeightSource cliff = (x, z) -> x >= 200 ? 55 : 65;      // A 10 block drop, below the line.
        assertEquals(0.0D, los(gentle, HF10, 6), 1e-9);
        assertEquals(6.0D, los(cliff, HF10, 6), 1e-9);
        TerrainMath.HeightSource stairs = (x, z) -> 65 - 10 * ((x / 40) % 2);  // Many cliffs: capped.
        assertEquals(18.0D, los(stairs, HF10, 6), 1e-9);
    }

    @Test
    void cliffsFarBelowAnElevatedPathDoNotCount() {
        TerrainMath.HeightSource cliff = (x, z) -> x >= 200 ? 55 : 65;
        assertEquals(0.0D, TerrainMath.lineOfSightLossDb(cliff, 0.5, 166.5, 0.5, 500.5, 166.5, 0.5, HF10, 8, 6, 18), 1e-9);
    }

    @Test
    void aWallCostsTheWorseOfItsEdgeAndItsCliffsNotBoth() {
        TerrainMath.HeightSource wall = (x, z) -> (x >= 240 && x < 260) ? 75 : 65; // 10 high: two cliffs, a low edge.
        double edgeOnly = los(wall, HF10, 0);
        assertEquals(Math.max(edgeOnly, 12.0D), los(wall, HF10, 6), 1e-9);
    }

    @Test
    void unknownTerrainIsClear() {
        assertEquals(0.0D, los((x, z) -> TerrainMath.UNKNOWN, VHF, 6), 1e-9);
    }

    @Test
    void theStationsOwnSurroundingsDoNotCount() {
        TerrainMath.HeightSource shack = (x, z) -> x < 6 ? 80 : 65; // A tall building right by the antenna.
        assertEquals(0.0D, los(shack, VHF, 0), 1e-9);
    }

    @Test
    void horizonSeesTheValleyWalls() {
        TerrainMath.HeightSource valley = (x, z) -> Math.abs(x) < 30 ? 65 : 121; // A deep valley running along Z.
        float[] h = TerrainMath.horizon(valley, 0.5, 66.5, 0.5, TerrainMath.HORIZON_RADIUS);
        double east = TerrainMath.bearing(0, 0, 100, 0), north = TerrainMath.bearing(0, 0, 0, -100);
        double fade = Math.toRadians(10);
        assertEquals(0.0D, TerrainMath.skipClearance(h, east, Math.toRadians(10), fade), 1e-9, "20m east over the wall");
        assertEquals(1.0D, TerrainMath.skipClearance(h, north, Math.toRadians(10), fade), 1e-9, "20m along the valley");
        assertEquals(1.0D, TerrainMath.skipClearance(h, east, Math.toRadians(70), fade), 1e-9, "80m NVIS straight up");
    }

    @Test
    void atlasAveragesCellsAndForgetsTheOldest() {
        TerrainAtlas atlas = new TerrainAtlas();
        short[] cells = new short[16];
        cells[5] = 90; // Cell (1, 1): blocks 4-7 in x and z.
        atlas.put(0, 0, cells, 2);
        assertEquals(90, atlas.surface(5, 6));
        assertEquals(0, atlas.surface(0, 0));
        assertEquals(TerrainMath.UNKNOWN, atlas.surface(-1, 0));
        atlas.put(1, 0, new short[16], 2);
        atlas.put(2, 0, new short[16], 2);
        assertEquals(2, atlas.chunkCount());
        assertEquals(TerrainMath.UNKNOWN, atlas.surface(5, 6), "the oldest chunk should be forgotten past the cap");
    }

    @Test
    void cacheKeepsUntilItExpires() {
        ExpiringCache<String, Integer> cache = new ExpiringCache<>(10);
        int[] computed = {0};
        cache.get("a", 0L, 100L, k -> ++computed[0]);
        cache.get("a", 50L, 100L, k -> ++computed[0]);
        assertEquals(1, computed[0]);
        cache.get("a", 150L, 100L, k -> ++computed[0]);
        assertEquals(2, computed[0]);
        cache.get("b", 0L, 0L, k -> ++computed[0]);
        cache.get("b", 0L, 0L, k -> ++computed[0]);
        assertEquals(4, computed[0], "a time to live of 0 keeps nothing");
    }
}
