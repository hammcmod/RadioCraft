package com.arrl.radiocraft.common.radio.terrain;

import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A coarse height map of one dimension: one surface height per 4 x 4 block cell (16 per chunk), copied from a chunk's
 * heightmap when it loads, refreshed while it stays loaded and once more when it unloads. So terrain is known for
 * every chunk loaded since the server started, and the voice thread can read it without touching the level.
 * <p>
 * A cell's height is the average surface of its 16 columns (leaves ignored), so a tree trunk or a lamp post barely
 * moves it while hills and solid buildings count in full. Each chunk's cells are an array that is replaced whole,
 * never changed, so readers on any thread see either the old or the new terrain. Past {@code maxChunks} chunks the
 * oldest ones are forgotten, and count as clear again until they load.
 */
public final class TerrainAtlas implements TerrainMath.HeightSource {

    /** Rough memory per remembered chunk: the 16 heights, the map entry and the age queue entry. */
    public static final int BYTES_PER_CHUNK = 170;

    private final Map<Long, short[]> chunks = new ConcurrentHashMap<>();
    private final Queue<Long> age = new ConcurrentLinkedQueue<>();
    private final Set<Long> loaded = ConcurrentHashMap.newKeySet();
    private Iterator<Long> refreshCursor = null; // Server thread only.

    @Override
    public int surface(int x, int z) {
        short[] cells = chunks.get(key(x >> 4, z >> 4));
        if(cells == null)
            return TerrainMath.UNKNOWN;
        return cells[((x & 15) >> 2) * 4 + ((z & 15) >> 2)];
    }

    /** Stores a chunk's cells (see {@link #sample}), forgetting the oldest chunks beyond {@code maxChunks}. */
    public void put(int chunkX, int chunkZ, short[] cells, int maxChunks) {
        Long key = key(chunkX, chunkZ); // Boxed once: the map and the age queue share it.
        if(chunks.put(key, cells) == null) {
            age.add(key);
            while(chunks.size() > maxChunks) {
                Long oldest = age.poll();
                if(oldest == null)
                    break;
                chunks.remove(oldest);
            }
        }
    }

    public int chunkCount() {
        return chunks.size();
    }

    public long estimatedBytes() {
        return (long)chunks.size() * BYTES_PER_CHUNK;
    }

    void markLoaded(LevelChunk chunk, int maxChunks) {
        loaded.add(key(chunk.getPos().x, chunk.getPos().z));
        put(chunk.getPos().x, chunk.getPos().z, sample(chunk), maxChunks);
    }

    void markUnloaded(LevelChunk chunk, int maxChunks) {
        loaded.remove(key(chunk.getPos().x, chunk.getPos().z));
        put(chunk.getPos().x, chunk.getPos().z, sample(chunk), maxChunks);
    }

    /**
     * Picks the next {@code count} loaded chunks to resample, in turn, so what players build or dig while a chunk is
     * loaded shows up. The caller looks each one up and passes it to {@link #put}. Server thread only.
     */
    long[] nextToRefresh(int count) {
        long[] out = new long[Math.min(count, loaded.size())];
        int n = 0;
        while(n < out.length) {
            if(refreshCursor == null || !refreshCursor.hasNext()) {
                refreshCursor = loaded.iterator();
                if(!refreshCursor.hasNext())
                    break;
            }
            out[n++] = refreshCursor.next();
        }
        return n == out.length ? out : Arrays.copyOf(out, n);
    }

    /** The surface height of each 4 x 4 cell of a loaded chunk: the average over its columns, ignoring leaves. */
    public static short[] sample(LevelChunk chunk) {
        short[] cells = new short[16];
        for(int cx = 0; cx < 4; cx++)
            for(int cz = 0; cz < 4; cz++) {
                int sum = 0;
                for(int dx = 0; dx < TerrainMath.CELL; dx++)
                    for(int dz = 0; dz < TerrainMath.CELL; dz++)
                        sum += chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx * TerrainMath.CELL + dx, cz * TerrainMath.CELL + dz) + 1;
                cells[cx * 4 + cz] = (short)Math.round(sum / 16.0F);
            }
        return cells;
    }

    public static long key(int chunkX, int chunkZ) {
        return ((long)chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    static int keyX(long key) {
        return (int)(key >> 32);
    }

    static int keyZ(long key) {
        return (int)key;
    }
}
