package com.arrl.radiocraft.common.be_networks;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.init.RadiocraftDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Faults on bare power wire (the maintainer's rules, 2026-10-24), while there is power behind them:
 * <ul>
 *     <li><b>Short</b>: a wire joined back to itself (a closed loop of power wire). It smokes, catches fire, and burns
 *     through after {@code wire_short_burn_seconds}, draining a large share of the stored energy every second.</li>
 *     <li><b>Wet</b>: plain wire touching water electrocutes whatever is in the water nearby and drains the batteries.</li>
 *     <li><b>Spark</b>: an open end of plain wire out in the rain sparks, drawing as much as a transmitting HF rig, and
 *     zaps anything that touches it (six times that draw for a second).</li>
 * </ul>
 * Waterproof wire neither gets wet nor sparks, but can short. Wires carry no block entity, so candidates are reported
 * by the wire itself (placed, a neighbour changed, a random tick) and kept here, server side and unsaved: after a
 * restart random ticks find them again.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public final class WireFaults {

	public enum Kind { SHORT, WET, SPARK }

	/** Wire blocks a loop search walks before giving up (and calling it no loop). */
	private static final int MAX_LOOP_SEARCH = 1024;
	/** Ticks between rechecks of a fault: is it still there, and how much power is behind it. */
	private static final int RECHECK_TICKS = 20;
	/** Ticks a zap keeps drawing its heavier load. */
	private static final int ZAP_TICKS = 20;

	private static final class Fault {
		Kind kind;
		/** Ticks this short has burned while powered. */
		int burning;
		/** Ticks left of a zap's heavy draw. */
		int zap;
		/** Power devices on the wire's run, from the last recheck. */
		List<PowerNetworkObject> supply = List.of();
		/** Energy stored behind the wire at the last recheck: 0 means nothing happens. */
		long stored;

		Fault(Kind kind) {
			this.kind = kind;
		}
	}

	private static final Map<ResourceKey<Level>, Map<BlockPos, Fault>> FAULTS = new HashMap<>();
	private static final Map<ResourceKey<Level>, Set<BlockPos>> PENDING = new HashMap<>();

	private WireFaults() {}

	public static boolean enabled() {
		return RadiocraftServerConfig.WIRE_HAZARDS_ENABLED.get();
	}

	/** Looks at the wire at {@code pos} on the next tick (a neighbour changed). Cheap: safe from shape updates. */
	public static void queue(ServerLevel level, BlockPos pos) {
		if(enabled())
			PENDING.computeIfAbsent(level.dimension(), k -> new LinkedHashSet<>()).add(pos.immutable());
	}

	/** Looks at the wire at {@code pos} now, starting or ending a fault there. */
	public static void check(ServerLevel level, BlockPos pos) {
		if(!enabled())
			return;
		Map<BlockPos, Fault> faults = FAULTS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
		Fault fault = faults.get(pos);
		Kind kind = fault != null && fault.kind == Kind.SHORT && isPowerWire(level.getBlockState(pos)) ? Kind.SHORT : classify(level, pos);
		if(kind == null)
			faults.remove(pos);
		else if(fault == null) {
			fault = new Fault(kind);
			recheckSupply(level, pos, fault);
			faults.put(pos.immutable(), fault);
		}
		else
			fault.kind = kind;
	}

	/** The fault at {@code pos}, or null. For tests and debugging. */
	public static Kind faultAt(Level level, BlockPos pos) {
		Map<BlockPos, Fault> faults = FAULTS.get(level.dimension());
		Fault fault = faults == null ? null : faults.get(pos);
		return fault == null ? null : fault.kind;
	}

	/** What is wrong with the wire at {@code pos}, or null for nothing (or no power wire there). */
	public static Kind classify(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if(!isPowerWire(state))
			return null;
		WireBlock wire = (WireBlock)state.getBlock();
		if(closesLoop(level, pos))
			return Kind.SHORT;
		if(wire.isWaterproof)
			return null;
		if(touchesWater(level, pos))
			return Kind.WET;
		if(level.isRainingAt(pos) && wireLinks(level, pos, state).size() + deviceLinks(level, pos, state) <= 1)
			return Kind.SPARK;
		return null;
	}

	private static boolean isPowerWire(BlockState state) {
		return state.getBlock() instanceof WireBlock wire && wire.isPower;
	}

	private static boolean touchesWater(Level level, BlockPos pos) {
		if(level.getFluidState(pos).is(FluidTags.WATER))
			return true;
		for(Direction direction : Direction.values()) {
			if(level.getFluidState(pos.relative(direction)).is(FluidTags.WATER))
				return true;
		}
		return false;
	}

	/** The power wires the wire at {@code pos} links to. */
	private static List<BlockPos> wireLinks(Level level, BlockPos pos, BlockState state) {
		List<BlockPos> out = new ArrayList<>();
		for(BlockPos link : ((WireBlock)state.getBlock()).getLinks(level, pos, state).keySet()) {
			if(isPowerWire(level.getBlockState(link)))
				out.add(link);
		}
		return out;
	}

	/** How many devices (batteries, radios) the wire at {@code pos} runs into. */
	private static int deviceLinks(Level level, BlockPos pos, BlockState state) {
		int devices = 0;
		for(BlockPos link : ((WireBlock)state.getBlock()).getLinks(level, pos, state).keySet()) {
			if(!isPowerWire(level.getBlockState(link)))
				devices++;
		}
		return devices;
	}

	/**
	 * True when the wire at {@code pos} joins a run of power wire back to itself: two of its neighbouring wires reach
	 * each other without going through it.
	 */
	public static boolean closesLoop(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if(!isPowerWire(state))
			return false;
		List<BlockPos> neighbours = wireLinks(level, pos, state);
		if(neighbours.size() < 2)
			return false;
		Set<BlockPos> seen = new HashSet<>();
		seen.add(pos);
		for(BlockPos start : neighbours) {
			if(seen.contains(start))
				return true; // Reached from an earlier neighbour without passing through pos.
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			queue.add(start);
			seen.add(start);
			while(!queue.isEmpty()) {
				if(seen.size() > MAX_LOOP_SEARCH)
					return false;
				BlockPos at = queue.poll();
				BlockState atState = level.getBlockState(at);
				for(BlockPos next : wireLinks(level, at, atState)) {
					if(next.equals(pos) || !seen.add(next))
						continue;
					queue.add(next);
				}
			}
		}
		return false;
	}

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if(!(event.getLevel() instanceof ServerLevel level))
			return;
		Set<BlockPos> pending = PENDING.remove(level.dimension());
		if(pending != null) {
			for(BlockPos pos : pending) {
				if(level.isLoaded(pos))
					check(level, pos);
			}
		}
		Map<BlockPos, Fault> faults = FAULTS.get(level.dimension());
		if(faults == null || faults.isEmpty())
			return;
		if(!enabled()) {
			faults.clear();
			return;
		}
		long time = level.getGameTime();
		List<BlockPos> burnt = new ArrayList<>();
		for(Iterator<Map.Entry<BlockPos, Fault>> it = faults.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<BlockPos, Fault> entry = it.next();
			BlockPos pos = entry.getKey();
			Fault fault = entry.getValue();
			if(!level.isLoaded(pos))
				continue;
			if((time + pos.hashCode()) % RECHECK_TICKS == 0) {
				Kind kind = fault.kind == Kind.SHORT && isPowerWire(level.getBlockState(pos)) ? Kind.SHORT : classify(level, pos);
				if(kind == null) {
					it.remove();
					continue;
				}
				fault.kind = kind;
				recheckSupply(level, pos, fault);
			}
			if(fault.stored <= 0L)
				continue;
			switch(fault.kind) {
				case SHORT -> {
					if(tickShort(level, pos, fault, time))
						burnt.add(pos);
				}
				case WET -> tickWet(level, pos, fault, time);
				case SPARK -> tickSpark(level, pos, fault, time);
			}
		}
		for(BlockPos pos : burnt) {
			faults.remove(pos);
			burnThrough(level, pos);
		}
	}

	private static void recheckSupply(ServerLevel level, BlockPos pos, Fault fault) {
		List<PowerNetworkObject> supply = new ArrayList<>();
		for(BENetworkObject object : WireUtils.getConnections(level, pos, o -> o instanceof PowerNetworkObject, WireUtils.powerWires()).keySet())
			supply.add((PowerNetworkObject)object);
		fault.supply = supply;
		fault.stored = stored(supply);
	}

	private static long stored(List<PowerNetworkObject> supply) {
		long stored = 0L;
		for(PowerNetworkObject object : supply)
			stored += object.getStorage().getEnergyStored();
		return stored;
	}

	/** Takes up to {@code amount} from the devices behind a fault, past their output limits (it is a fault). */
	private static long drain(Fault fault, long amount) {
		long drained = 0L;
		for(PowerNetworkObject object : fault.supply) {
			if(drained >= amount)
				break;
			int have = object.getStorage().getEnergyStored();
			int take = (int)Math.min(have, amount - drained);
			if(take > 0) {
				object.getStorage().setEnergy(have - take);
				drained += take;
			}
		}
		fault.stored = Math.max(0L, fault.stored - drained);
		return drained;
	}

	/** Once a second, a fault's share of everything stored behind it. */
	private static void drainShare(Fault fault, long time, BlockPos pos) {
		if((time + pos.hashCode()) % 20 == 0)
			drain(fault, Math.max(1L, (long)Math.ceil(stored(fault.supply) * RadiocraftServerConfig.WIRE_FAULT_DRAIN.get())));
	}

	/** Smoke, then fire; true once it has burned through. */
	private static boolean tickShort(ServerLevel level, BlockPos pos, Fault fault, long time) {
		fault.burning++;
		drainShare(fault, time, pos);
		double x = pos.getX() + 0.5D, y = pos.getY() + 0.1D, z = pos.getZ() + 0.5D;
		if(fault.burning % 4 == 0)
			level.sendParticles(ParticleTypes.SMOKE, x, y, z, 2, 0.2D, 0.05D, 0.2D, 0.01D);
		if(fault.burning == 1)
			level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F); // The fizz of a short.
		if(fault.burning > 20) { // On fire.
			if(fault.burning % 3 == 0)
				level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.2D, 0.05D, 0.2D, 0.005D);
			if(fault.burning % 30 == 0)
				level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.6F, 1.0F);
		}
		return fault.burning >= RadiocraftServerConfig.WIRE_SHORT_BURN_SECONDS.get() * 20;
	}

	/** The short has burned through: the wire is gone, leaving fire where fire can burn. */
	private static void burnThrough(ServerLevel level, BlockPos pos) {
		if(!isPowerWire(level.getBlockState(pos)))
			return;
		level.removeBlock(pos, false);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D, 6, 0.2D, 0.1D, 0.2D, 0.02D);
		if(BaseFireBlock.canBePlacedAt(level, pos, Direction.UP))
			level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
	}

	/** Electrocutes whatever is in the water nearby, once a second, and drains the batteries. */
	private static void tickWet(ServerLevel level, BlockPos pos, Fault fault, long time) {
		if((time + pos.hashCode()) % 20 != 0)
			return;
		drainShare(fault, time, pos);
		double range = RadiocraftServerConfig.WIRE_WATER_RANGE.get();
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5D, pos.getY() + 0.3D, pos.getZ() + 0.5D, 6, 0.4D, 0.2D, 0.4D, 0.1D);
		for(LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(range))) {
			if(entity.isInWater() && entity.position().distanceTo(pos.getCenter()) <= range) {
				entity.hurt(RadiocraftDamageTypes.electrocution(level), RadiocraftServerConfig.WIRE_WATER_DAMAGE.get().floatValue());
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 8, 0.3D, 0.4D, 0.3D, 0.2D);
			}
		}
	}

	/** Sparks, draws a transmitting HF rig's worth, and zaps anything touching it. */
	private static void tickSpark(ServerLevel level, BlockPos pos, Fault fault, long time) {
		long draw = sparkDraw();
		if(fault.zap > 0) {
			fault.zap--;
			draw *= RadiocraftServerConfig.WIRE_ZAP_MULTIPLIER.get();
		}
		drain(fault, draw);
		double x = pos.getX() + 0.5D, y = pos.getY() + 0.1D, z = pos.getZ() + 0.5D;
		if(level.random.nextInt(4) == 0)
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 2, 0.1D, 0.05D, 0.1D, 0.05D);
		if(level.random.nextInt(30) == 0)
			level.playSound(null, pos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.25F, 1.8F + level.random.nextFloat() * 0.2F);
		for(LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos))) {
			if(entity.hurt(RadiocraftDamageTypes.electrocution(level), RadiocraftServerConfig.WIRE_ZAP_DAMAGE.get().floatValue())) {
				fault.zap = ZAP_TICKS;
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY() + 0.3D, entity.getZ(), 12, 0.2D, 0.3D, 0.2D, 0.2D);
				level.playSound(null, pos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.8F, 1.4F);
			}
		}
	}

	/** What a sparking wire draws per tick: a transmitting HF rig (the 20m radio). */
	public static long sparkDraw() {
		return CommonConfig.HF_RADIO_20M_TRANSMIT_TICK.get();
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		FAULTS.clear();
		PENDING.clear();
	}
}
