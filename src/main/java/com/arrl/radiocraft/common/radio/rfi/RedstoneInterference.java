package com.arrl.radiocraft.common.radio.rfi;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.WireUtils;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.entities.IAntennaWire;
import com.arrl.radiocraft.common.radio.Band;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redstone interference, receive side: redstone switching near a station's feedline or antenna raises that radio's
 * static. Real switching electronics (power supplies, motors, arcing contacts) radiate broadband noise that a nearby
 * antenna or feedline picks up; steady DC is silent. So the trigger is a <em>change</em>, not a powered block: one lever
 * flip is a small pop, a redstone clock is a constant buzz.
 * <p>
 * Performance: every block update on the server fires {@link BlockEvent.NeighborNotifyEvent}, so the handler must be
 * cheap in the common case. Each powered radio registers a {@link PickupZone} under every chunk its zone touches; the
 * handler does one map lookup per event and returns when the chunk has no zone. Only then is the block checked for
 * being a redstone component and the distance to the station worked out (a few dozen points).
 * <p>
 * The size of each pop depends on the band (long wavelengths pick up far more: 80/40m full, 20m half, 10m and VHF a
 * tenth), on the distance, and on the server config, which can also disable the feature or restrict it to stations with
 * a bad SWR.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public class RedstoneInterference {

	/** Noise added by one switch of a signal source right on the wire, before band and config scaling. */
	public static final double POP = 0.3D;
	/** Blocks that are merely driven (pistons, doors, note blocks) rather than signal sources count this much of a pop. */
	public static final double DRIVEN_BLOCK_FACTOR = 0.3D;
	private static final int MAX_WIRES = 64;
	private static final int MAX_COAX = 256;

	private static final Map<ResourceKey<Level>, Map<Long, List<PickupZone>>> ZONES = new HashMap<>();
	private static final Map<ResourceKey<Level>, Map<BlockPos, PickupZone>> ZONES_BY_RADIO = new HashMap<>();

	// -------------------- REGISTRY --------------------

	/** Replaces the zone registered for {@code zone.radioPos()}. Server thread only. */
	public static void register(Level level, PickupZone zone) {
		unregister(level, zone.radioPos());
		Map<Long, List<PickupZone>> byChunk = ZONES.computeIfAbsent(level.dimension(), k -> new HashMap<>());
		for(long chunk : zone.chunks())
			byChunk.computeIfAbsent(chunk, k -> new ArrayList<>(2)).add(zone);
		ZONES_BY_RADIO.computeIfAbsent(level.dimension(), k -> new HashMap<>()).put(zone.radioPos(), zone);
	}

	public static void unregister(Level level, BlockPos radioPos) {
		Map<BlockPos, PickupZone> byRadio = ZONES_BY_RADIO.get(level.dimension());
		if(byRadio == null)
			return;
		PickupZone old = byRadio.remove(radioPos);
		if(old == null)
			return;
		Map<Long, List<PickupZone>> byChunk = ZONES.get(level.dimension());
		if(byChunk == null)
			return;
		for(long chunk : old.chunks()) {
			List<PickupZone> list = byChunk.get(chunk);
			if(list != null) {
				list.remove(old);
				if(list.isEmpty())
					byChunk.remove(chunk);
			}
		}
	}

	/** The zone currently registered for the radio at {@code radioPos}, if any. */
	public static PickupZone getZone(Level level, BlockPos radioPos) {
		Map<BlockPos, PickupZone> byRadio = ZONES_BY_RADIO.get(level.dimension());
		return byRadio == null ? null : byRadio.get(radioPos);
	}

	/**
	 * Every station with a zone in {@code level}: the loaded radios that are powered with one antenna, which is also
	 * the set that plays static. A snapshot, safe to iterate while radios register and unregister.
	 */
	public static List<PickupZone> getZones(Level level) {
		Map<BlockPos, PickupZone> byRadio = ZONES_BY_RADIO.get(level.dimension());
		return byRadio == null ? List.of() : List.copyOf(byRadio.values());
	}

	@SubscribeEvent
	public static void levelUnload(LevelEvent.Unload event) {
		if(event.getLevel() instanceof Level level) {
			ZONES.remove(level.dimension());
			ZONES_BY_RADIO.remove(level.dimension());
		}
	}

	// -------------------- EVENTS --------------------

	@SubscribeEvent
	public static void neighborNotify(BlockEvent.NeighborNotifyEvent event) {
		if(!(event.getLevel() instanceof Level level) || level.isClientSide)
			return;
		Map<Long, List<PickupZone>> byChunk = ZONES.get(level.dimension());
		if(byChunk == null)
			return;
		BlockPos pos = event.getPos();
		List<PickupZone> zones = byChunk.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
		if(zones == null)
			return;

		double weight = switchWeight(event.getState());
		if(weight <= 0.0D || !RadiocraftServerConfig.REDSTONE_INTERFERENCE_ENABLED.get())
			return;

		for(PickupZone zone : zones) {
			double coupling = zone.coupling(pos);
			if(coupling > 0.0D && level.getBlockEntity(zone.radioPos()) instanceof RadioBlockEntity radio)
				radio.addInterference(POP * weight * coupling);
		}
	}

	/**
	 * How noisy one update from {@code state} is: 1 for redstone signal sources (dust, repeaters, comparators, torches,
	 * levers, observers...), {@link #DRIVEN_BLOCK_FACTOR} for blocks that only get driven by redstone, 0 for anything
	 * else (a block being placed or broken is not electrical).
	 */
	public static double switchWeight(BlockState state) {
		if(state.isSignalSource())
			return 1.0D;
		if(state.hasProperty(BlockStateProperties.POWERED) || state.hasProperty(BlockStateProperties.EXTENDED) || state.hasProperty(BlockStateProperties.TRIGGERED))
			return DRIVEN_BLOCK_FACTOR;
		return 0.0D;
	}

	// -------------------- SCALING --------------------

	/**
	 * How much of the noise a band hears: the long HF bands pick up nearly all of it, 20m half, 10m and VHF a tenth.
	 * Takes the band's wavelength in metres from its name ("80m").
	 */
	public static double bandFactor(Band band) {
		if(band == null)
			return 1.0D;
		int wavelength;
		try {
			wavelength = Integer.parseInt(band.name().replaceAll("[^0-9]", ""));
		}
		catch(NumberFormatException e) {
			return 1.0D;
		}
		if(wavelength >= 40)
			return 1.0D;
		if(wavelength >= 20)
			return 0.5D;
		return 0.1D;
	}

	/**
	 * Config scaling for a station: the admin's strength multiplier, or 0 when the feature is off or restricted to bad
	 * SWR and this station's match is fine.
	 */
	public static double configFactor(double swr) {
		if(!RadiocraftServerConfig.REDSTONE_INTERFERENCE_ENABLED.get())
			return 0.0D;
		if(RadiocraftServerConfig.REDSTONE_INTERFERENCE_ONLY_BAD_SWR.get() && swr < RadiocraftServerConfig.REDSTONE_INTERFERENCE_BAD_SWR.get())
			return 0.0D;
		return RadiocraftServerConfig.REDSTONE_INTERFERENCE_STRENGTH.get();
	}

	// -------------------- ZONE BUILDING --------------------

	/**
	 * Works out the pickup zone of the radio at {@code radioPos} with antenna {@code antenna}: the coax between them
	 * and the antenna's wires. Null if there is nothing to pick up with.
	 */
	public static PickupZone buildZone(Level level, BlockPos radioPos, AntennaNetworkObject antenna) {
		List<BlockPos> feedline = new ArrayList<>();
		feedline.add(radioPos);
		collectCoax(level, radioPos, feedline);

		List<BlockPos> antennaPoints = new ArrayList<>();
		collectAntennaWires(level, antenna.getPos(), antennaPoints);
		return PickupZone.of(radioPos, feedline, antennaPoints);
	}

	/** Follows coax wire blocks outwards from {@code start} until they end at a network object, adding each wire. */
	private static void collectCoax(Level level, BlockPos start, List<BlockPos> out) {
		Set<WireBlock> coax = Set.of(WireUtils.coaxWires());
		Set<BlockPos> seen = new HashSet<>();
		seen.add(start);
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for(Direction dir : Direction.values())
			queue.add(start.relative(dir));

		while(!queue.isEmpty() && out.size() < MAX_COAX) {
			BlockPos pos = queue.poll();
			if(!seen.add(pos))
				continue;
			BlockState state = level.getBlockState(pos);
			if(!coax.contains(state.getBlock()))
				continue;
			out.add(pos);
			for(Direction dir : ((WireBlock)state.getBlock()).getConnections(level, pos)) {
				BlockPos next = pos.relative(dir);
				BENetworkObject obj = IBENetworks.getObject(level, next);
				if(obj == null)
					queue.add(next); // Wires end at network objects (radio, antenna, tuner); don't walk through them.
			}
		}
	}

	/** Samples every antenna wire reachable from {@code center} (the balun/pole the coax ends at). */
	private static void collectAntennaWires(Level level, BlockPos center, List<BlockPos> out) {
		out.add(center);
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(center);
		int wires = 0;
		while(!queue.isEmpty() && wires < MAX_WIRES) {
			BlockPos pos = queue.poll();
			if(!visited.add(pos))
				continue;
			for(IAntennaWire wire : AntennaWire.getWires(level, pos)) {
				BlockPos other = wire.getEndPos();
				if(visited.contains(other))
					continue;
				PickupZone.sampleLine(pos, other, out);
				queue.add(other);
				wires++;
			}
		}
	}
}
