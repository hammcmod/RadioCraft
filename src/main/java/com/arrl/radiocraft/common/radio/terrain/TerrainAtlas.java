package com.arrl.radiocraft.common.radio.terrain;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A coarse height map of one dimension: one surface height per 4 x 4 block cell (16 per chunk), copied from a chunk's
 * heightmap when it loads, refreshed while it stays loaded and once more when it unloads. It is saved with the world
 * ({@code data/radiocraft_terrain.dat} in the dimension's folder), so terrain is known for every chunk ever loaded,
 * and the voice thread can read it without touching the level.
 * <p>
 * A cell's height is the average surface of its 16 columns (leaves ignored), so a tree trunk or a lamp post barely
 * moves it while hills and solid buildings count in full. Each chunk is a record that is replaced whole, never
 * changed, so readers on any thread see either the old or the new terrain. Each carries a stamp from a counter that
 * moves only when terrain really changes, so a path worked out earlier can tell whether the chunks it crossed are
 * still the same ({@link #changedSince}). Past {@code maxChunks} chunks the oldest are forgotten, and count as clear
 * again until they load.
 */
public final class TerrainAtlas extends SavedData implements TerrainMath.HeightSource {

    /** Rough memory per remembered chunk: the 16 heights, their record, the map entry and the age queue entry. */
    public static final int BYTES_PER_CHUNK = 190;
    public static final String SAVE_NAME = "radiocraft_terrain";

    private record Chunk(short[] cells, long stamp) {}

    private final Map<Long, Chunk> chunks = new ConcurrentHashMap<>();
    private final Queue<Long> age = new ConcurrentLinkedQueue<>();
    private final Set<Long> loaded = ConcurrentHashMap.newKeySet();
    private final AtomicLong changes = new AtomicLong();
    private Iterator<Long> refreshCursor = null; // Server thread only.

    public static SavedData.Factory<TerrainAtlas> factory() {
        return new SavedData.Factory<>(TerrainAtlas::new, TerrainAtlas::load, null);
    }

    @Override
    public int surface(int x, int z) {
        Chunk chunk = chunks.get(key(x >> 4, z >> 4));
        if(chunk == null)
            return TerrainMath.UNKNOWN;
        return chunk.cells()[((x & 15) >> 2) * 4 + ((z & 15) >> 2)];
    }

    /**
     * Stores a chunk's cells (see {@link #sample}), forgetting the oldest chunks beyond {@code maxChunks}. The same
     * terrain again changes nothing (and moves no stamp).
     */
    public void put(int chunkX, int chunkZ, short[] cells, int maxChunks) {
        Long key = key(chunkX, chunkZ); // Boxed once: the map and the age queue share it.
        Chunk old = chunks.get(key);
        if(old != null && Arrays.equals(old.cells(), cells))
            return;
        chunks.put(key, new Chunk(cells, changes.incrementAndGet()));
        setDirty();
        if(old == null) {
            age.add(key);
            while(chunks.size() > maxChunks) {
                Long oldest = age.poll();
                if(oldest == null)
                    break;
                chunks.remove(oldest);
            }
        }
    }

    /** The current change count: a later {@link #changedSince} with it tells whether any chunk has changed since. */
    public long stamp() {
        return changes.get();
    }

    /** True if the chunk at these chunk coordinates changed (or first appeared) after {@code stamp}. */
    public boolean changedSince(long chunkKey, long stamp) {
        Chunk chunk = chunks.get(chunkKey);
        return chunk != null && chunk.stamp() > stamp;
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

    /** Saved oldest first, so the forgetting order survives a restart: chunk keys, and each chunk's 16 heights packed two to an int. */
    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        long[] keys = new long[chunks.size()];
        int[] cells = new int[keys.length * 8];
        int n = 0;
        for(Long key : age) {
            Chunk chunk = chunks.get(key);
            if(chunk == null || n == keys.length)
                continue;
            keys[n] = key;
            for(int i = 0; i < 8; i++)
                cells[n * 8 + i] = (chunk.cells()[i * 2] & 0xFFFF) << 16 | (chunk.cells()[i * 2 + 1] & 0xFFFF);
            n++;
        }
        tag.putLongArray("keys", Arrays.copyOf(keys, n));
        tag.putIntArray("cells", Arrays.copyOf(cells, n * 8));
        return tag;
    }

    public static TerrainAtlas load(CompoundTag tag, HolderLookup.Provider registries) {
        TerrainAtlas atlas = new TerrainAtlas();
        long[] keys = tag.getLongArray("keys");
        int[] cells = tag.getIntArray("cells");
        for(int n = 0; n < keys.length && n * 8 + 7 < cells.length; n++) {
            short[] chunk = new short[16];
            for(int i = 0; i < 8; i++) {
                chunk[i * 2] = (short)(cells[n * 8 + i] >>> 16);
                chunk[i * 2 + 1] = (short)cells[n * 8 + i];
            }
            Long key = keys[n];
            atlas.chunks.put(key, new Chunk(chunk, 0L));
            atlas.age.add(key);
        }
        return atlas;
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
