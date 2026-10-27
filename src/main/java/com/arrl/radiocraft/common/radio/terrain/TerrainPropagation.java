package com.arrl.radiocraft.common.radio.terrain;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.BandUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Terrain-aware propagation in game: keeps a {@link TerrainAtlas} per dimension from chunk loads, and gives the antenna
 * types a factor for how much of a signal the terrain lets through (see doc/TerrainPropagationResearch.md).
 * <p>
 * Called on Simple Voice Chat's thread for every voice packet to every receiver, so it never reads the level: only
 * the atlas. A path's loss and a station's horizon are kept for {@code terrain_recalculate_seconds}, looked up by 4
 * block cell, so a talking station costs a map lookup per receiver and a moving one is worked out again as it walks.
 * Dimensions with a ceiling (the Nether) have no sky to measure and are left alone. {@code terrain_enabled = false}
 * stops all of it: no atlas, no hooks doing work, a factor of 1.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public final class TerrainPropagation {

    /** Height of a station's antenna above its block (a handheld at eye level, a balun's wires just above it). */
    private static final double ANTENNA_HEIGHT = 1.5D;
    /** Cached results beyond this many are dropped all at once, which only costs a recalculation. */
    private static final int MAX_CACHE = 50_000;

    private static final Map<ResourceKey<Level>, TerrainAtlas> ATLASES = new ConcurrentHashMap<>();
    private static final ExpiringCache<PathKey, PathLoss> PATHS = new ExpiringCache<>(MAX_CACHE);
    private static final ExpiringCache<SpotKey, float[]> HORIZONS = new ExpiringCache<>(MAX_CACHE);

    /** Running cost counters, for /rcdebug terrain and the benchmark. */
    public static final LongAdder CALLS = new LongAdder();
    public static final LongAdder CALL_NANOS = new LongAdder();
    public static final LongAdder PATHS_WORKED_OUT = new LongAdder();
    public static final LongAdder HORIZONS_WORKED_OUT = new LongAdder();

    private TerrainPropagation() {}

    private record PathKey(ResourceKey<Level> dimension, long a, long b, String band) {}
    private record SpotKey(ResourceKey<Level> dimension, long spot) {}

    public static boolean enabled() {
        return RadiocraftServerConfig.TERRAIN_ENABLED.get();
    }

    /**
     * The atlas for a level, or null if terrain is off, the level has a ceiling, or nothing has opened its map yet.
     * Safe on any thread: it only reads.
     */
    public static TerrainAtlas atlas(Level level) {
        if(level == null || !enabled() || level.dimensionType().hasCeiling())
            return null;
        return ATLASES.get(level.dimension());
    }

    /** The atlas for a level, loading it from the world's save or starting it. Server thread only. */
    public static TerrainAtlas openAtlas(ServerLevel level) {
        if(!enabled() || level.dimensionType().hasCeiling())
            return null;
        return ATLASES.computeIfAbsent(level.dimension(), k -> level.getDataStorage().computeIfAbsent(TerrainAtlas.factory(), TerrainAtlas.SAVE_NAME));
    }

    /**
     * Share of a signal the terrain lets through between the packet's source and {@code destination}: line of sight
     * ({@code skip} false) through hills and over cliffs, or skip out over both stations' horizons. 1 when terrain is
     * off.
     */
    public static double factor(IAntennaPacket packet, BlockPos destination, boolean skip) {
        IAntenna source = packet.getSource();
        if(source == null || source.getAntennaPos() == null || !enabled())
            return 1.0D;
        long start = System.nanoTime();
        IAntenna target = packet.getDestination();
        boolean fixed = source.getPlayer() == null && target != null && target.getPlayer() == null; // Two stations that never move.
        double factor = factor(packet.getLevel(), packet.getBand(), source.getAntennaPos().position(), source.isIndoors(),
                destination, target != null && target.isIndoors(), skip, fixed);
        CALLS.increment();
        CALL_NANOS.add(System.nanoTime() - start);
        return factor;
    }

    public static double factor(Level level, Band band, BlockPos from, boolean fromIndoors, BlockPos to, boolean toIndoors, boolean skip) {
        return factor(level, band, from, fromIndoors, to, toIndoors, skip, false);
    }

    /** As above; {@code fixed} for two stations that never move, whose path is kept until terrain on it changes. */
    public static double factor(Level level, Band band, BlockPos from, boolean fromIndoors, BlockPos to, boolean toIndoors, boolean skip, boolean fixed) {
        TerrainAtlas atlas = atlas(level);
        if(atlas == null || band == null)
            return 1.0D;
        boolean vhf = band.isVhf();
        double indoorDb = vhf ? RadiocraftServerConfig.TERRAIN_INDOOR_VHF_LOSS_DB.get() : RadiocraftServerConfig.TERRAIN_INDOOR_HF_LOSS_DB.get();
        int indoors = (fromIndoors ? 1 : 0) + (toIndoors ? 1 : 0);
        if(skip) {
            double factor = TerrainMath.dbToFactor(indoors * indoorDb * 2.0D);
            if(RadiocraftServerConfig.TERRAIN_SKY_VIEW.get() && !vhf) {
                double takeoff = Math.toRadians(takeoffDegrees(band));
                double fade = Math.toRadians(RadiocraftServerConfig.TERRAIN_SKY_FADE.get());
                factor *= TerrainMath.skipClearance(horizon(level, atlas, from), TerrainMath.bearing(from.getX(), from.getZ(), to.getX(), to.getZ()), takeoff, fade);
                factor *= TerrainMath.skipClearance(horizon(level, atlas, to), TerrainMath.bearing(to.getX(), to.getZ(), from.getX(), from.getZ()), takeoff, fade);
            }
            return factor;
        }
        double db = indoors * indoorDb;
        if(RadiocraftServerConfig.TERRAIN_LINE_OF_SIGHT.get())
            db += pathLossDb(level, atlas, band, from, to, fixed);
        return TerrainMath.dbToFactor(db);
    }

    /** Terrain loss in dB on the line-of-sight path, kept for a while per pair of cells. */
    public static double pathLossDb(Level level, TerrainAtlas atlas, Band band, BlockPos from, BlockPos to) {
        return pathLossDb(level, atlas, band, from, to, false);
    }

    /**
     * As above. A path between two {@code fixed} stations is kept past its time as long as no chunk it crosses has
     * changed: checking that costs a few map reads, against working the whole path out again.
     */
    public static double pathLossDb(Level level, TerrainAtlas atlas, Band band, BlockPos from, BlockPos to, boolean fixed) {
        long a = cell(from), b = cell(to);
        return PATHS.get(new PathKey(level.dimension(), Math.min(a, b), Math.max(a, b), band.name()), ttl(), key -> {
            long stamp = atlas.stamp();
            double wavelength = BandUtils.getWavelengthMetersFromFrequencyHertz(band.minFrequency());
            double cliffDb = band.isVhf() ? 0.0D : RadiocraftServerConfig.TERRAIN_CLIFF_LOSS_DB.get() * Math.sqrt(10.0D / wavelength);
            PATHS_WORKED_OUT.increment();
            double db = TerrainMath.lineOfSightLossDb(atlas,
                    from.getX() + 0.5D, from.getY() + ANTENNA_HEIGHT, from.getZ() + 0.5D,
                    to.getX() + 0.5D, to.getY() + ANTENNA_HEIGHT, to.getZ() + 0.5D,
                    wavelength, RadiocraftServerConfig.TERRAIN_CLIFF_RISE.get(), cliffDb, RadiocraftServerConfig.TERRAIN_CLIFF_MAX_LOSS_DB.get());
            return new PathLoss(db, stamp, fixed ? chunksOnPath(from, to) : null);
        }, kept -> kept.chunks() != null && unchanged(atlas, kept)).db();
    }

    /** A path's loss, with what is needed to tell later whether its terrain changed (fixed stations only). */
    private record PathLoss(double db, long stamp, long[] chunks) {}

    private static boolean unchanged(TerrainAtlas atlas, PathLoss kept) {
        for(long chunk : kept.chunks())
            if(atlas.changedSince(chunk, kept.stamp()))
                return false;
        return true;
    }

    /** The chunks a straight path crosses, sampled every 8 blocks. */
    static long[] chunksOnPath(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
        int steps = Math.max(1, (int)Math.ceil(Math.sqrt(dx * dx + dz * dz) / 8.0D));
        java.util.LinkedHashSet<Long> keys = new java.util.LinkedHashSet<>();
        for(int i = 0; i <= steps; i++) {
            double f = (double)i / steps;
            keys.add(TerrainAtlas.key(((int)Math.floor(from.getX() + dx * f)) >> 4, ((int)Math.floor(from.getZ() + dz * f)) >> 4));
        }
        return keys.stream().mapToLong(Long::longValue).toArray();
    }

    /** A station's horizon profile, kept for a while per cell. */
    public static float[] horizon(Level level, TerrainAtlas atlas, BlockPos pos) {
        return HORIZONS.get(new SpotKey(level.dimension(), cell(pos)), ttl(), key -> {
            HORIZONS_WORKED_OUT.increment();
            return TerrainMath.horizon(atlas, pos.getX() + 0.5D, pos.getY() + ANTENNA_HEIGHT, pos.getZ() + 0.5D, TerrainMath.HORIZON_RADIUS);
        });
    }

    private static long ttl() {
        return (long)(RadiocraftServerConfig.TERRAIN_RECALCULATE_SECONDS.get() * 1e9D);
    }

    /** Takeoff angle of a band's skip signal, in degrees. */
    public static double takeoffDegrees(Band band) {
        return switch(band.name()) {
            case "10m" -> RadiocraftServerConfig.TERRAIN_TAKEOFF_10M.get();
            case "20m" -> RadiocraftServerConfig.TERRAIN_TAKEOFF_20M.get();
            case "40m" -> RadiocraftServerConfig.TERRAIN_TAKEOFF_40M.get();
            default -> RadiocraftServerConfig.TERRAIN_TAKEOFF_80M.get();
        };
    }

    /** The 4 x 4 x 4 block cell a position is in, packed like a BlockPos. */
    private static long cell(BlockPos pos) {
        return BlockPos.asLong(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2);
    }

    /** Forgets every kept path and horizon, so the next packet works them out from the atlas again. */
    public static void clearCaches() {
        PATHS.clear();
        HORIZONS.clear();
    }

    public static void resetCounters() {
        CALLS.reset();
        CALL_NANOS.reset();
        PATHS_WORKED_OUT.reset();
        HORIZONS_WORKED_OUT.reset();
    }

    /** Re-reads the loaded chunks from {@code min} to {@code max} into the atlas now, rather than in turn. */
    public static void refresh(ServerLevel level, BlockPos min, BlockPos max) {
        TerrainAtlas atlas = openAtlas(level);
        if(atlas == null)
            return;
        for(int cx = Math.min(min.getX(), max.getX()) >> 4; cx <= Math.max(min.getX(), max.getX()) >> 4; cx++)
            for(int cz = Math.min(min.getZ(), max.getZ()) >> 4; cz <= Math.max(min.getZ(), max.getZ()) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if(chunk != null)
                    atlas.put(cx, cz, TerrainAtlas.sample(chunk), maxChunks());
            }
        clearCaches();
    }

    private static int maxChunks() {
        return RadiocraftServerConfig.TERRAIN_ATLAS_MAX_CHUNKS.get();
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if(event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            TerrainAtlas atlas = openAtlas(level);
            if(atlas != null)
                atlas.markLoaded(chunk, maxChunks());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if(event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            TerrainAtlas atlas = openAtlas(level);
            if(atlas != null)
                atlas.markUnloaded(chunk, maxChunks());
        }
    }

    /** Re-reads a few loaded chunks each tick, in turn, so building and digging show up. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if(!enabled()) {
            if(!ATLASES.isEmpty()) { // Switched off while running: let the memory go.
                ATLASES.clear();
                clearCaches();
            }
            return;
        }
        int budget = RadiocraftServerConfig.TERRAIN_REFRESH_CHUNKS_PER_TICK.get();
        if(budget <= 0)
            return;
        for(ServerLevel level : event.getServer().getAllLevels()) {
            TerrainAtlas atlas = ATLASES.get(level.dimension());
            if(atlas == null)
                continue;
            for(long key : atlas.nextToRefresh(budget)) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(TerrainAtlas.keyX(key), TerrainAtlas.keyZ(key));
                if(chunk != null)
                    atlas.put(chunk.getPos().x, chunk.getPos().z, TerrainAtlas.sample(chunk), maxChunks());
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ATLASES.clear();
        clearCaches();
        com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager.clearAll(); // The next world starts with no antennas on the air.
        com.arrl.radiocraft.common.radio.TransmitterLog.clear();
        com.arrl.radiocraft.common.radio.VoiceTransmitters.LISTENERS.clear();
    }
}
