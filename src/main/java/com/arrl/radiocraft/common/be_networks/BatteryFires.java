package com.arrl.radiocraft.common.be_networks;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.blockentities.LargeBatteryBlockEntity;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.capabilities.BENetworksCapability;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Burning large batteries (the maintainer's rules, 2026-10-24). A battery catches fire from fire or lava beside it, a
 * flint and steel, or lightning striking power wire near it that runs to it. Once alight it cannot be put out: it
 * burns until it is consumed ({@code battery_burn_seconds}), setting fire around it and spreading to batteries it
 * touches. Water touching it holds the flames down (it hisses, and keeps being consumed), but take the water away
 * before it is gone and it flares up again. A battery consumed in a group of {@code battery_explode_group} or more
 * touching batteries explodes, harder for a bigger group, and about a quarter of those explosions start fires.
 * <p>
 * How long a battery has left is kept on its block entity (so it survives a restart); this keeps the set of burning
 * ones to tick.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public final class BatteryFires {

	/** Touching batteries a group search walks before stopping. */
	private static final int MAX_GROUP = 64;

	private static final Map<ResourceKey<Level>, Set<BlockPos>> BURNING = new HashMap<>();

	private BatteryFires() {}

	public static boolean enabled() {
		return RadiocraftServerConfig.BATTERY_FIRES_ENABLED.get();
	}

	/** Sets the battery at {@code pos} alight, if it is not already. True if it is now burning. */
	public static boolean ignite(Level level, BlockPos pos) {
		if(!enabled() || !(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof LargeBatteryBlockEntity battery))
			return false;
		if(!battery.isBurning()) {
			battery.setBurnTicks(RadiocraftServerConfig.BATTERY_BURN_SECONDS.get() * 20);
			level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
		}
		track(server, pos);
		return true;
	}

	/** Keeps ticking a battery that is burning (one loaded from the save still alight). */
	public static void track(ServerLevel level, BlockPos pos) {
		BURNING.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
	}

	public static boolean isBurning(Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof LargeBatteryBlockEntity battery && battery.isBurning();
	}

	/** True for a block that sets a battery beside it alight: fire, or lava. */
	public static boolean isIgniter(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.is(BlockTags.FIRE) || level.getFluidState(pos).is(FluidTags.LAVA);
	}

	/** A battery placed or with a changed neighbour: alight if fire or lava touches it. */
	public static void checkNeighbours(Level level, BlockPos pos) {
		if(level.isClientSide || !enabled())
			return;
		for(Direction direction : Direction.values()) {
			if(isIgniter(level, pos.relative(direction))) {
				ignite(level, pos);
				return;
			}
		}
	}

	private static boolean touchesWater(Level level, BlockPos pos) {
		for(Direction direction : Direction.values()) {
			if(level.getFluidState(pos.relative(direction)).is(FluidTags.WATER))
				return true;
		}
		return false;
	}

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if(!(event.getLevel() instanceof ServerLevel level))
			return;
		Set<BlockPos> burning = BURNING.get(level.dimension());
		if(burning == null || burning.isEmpty())
			return;
		List<BlockPos> consumed = new ArrayList<>();
		List<BlockPos> spread = new ArrayList<>();
		for(BlockPos pos : List.copyOf(burning)) {
			if(!level.isLoaded(pos))
				continue;
			if(!(level.getBlockEntity(pos) instanceof LargeBatteryBlockEntity battery) || !battery.isBurning()) {
				burning.remove(pos);
				continue;
			}
			int left = battery.getBurnTicks() - 1;
			battery.setBurnTicks(Math.max(left, 0));
			drainCharge(level, pos, left);
			if(left <= 0) {
				consumed.add(pos);
				continue;
			}
			if(touchesWater(level, pos))
				tickDoused(level, pos, left);
			else
				tickFlames(level, pos, left, spread);
		}
		for(BlockPos pos : spread)
			ignite(level, pos);
		for(BlockPos pos : consumed) {
			burning.remove(pos);
			consume(level, pos);
		}
	}

	/** The charge burns away with the battery: none left when it is consumed. */
	private static void drainCharge(ServerLevel level, BlockPos pos, int left) {
		if(BENetworksCapability.get(level).getObject(pos) instanceof BatteryNetworkObject battery) {
			int energy = battery.getStorage().getEnergyStored();
			if(energy > 0)
				battery.getStorage().setEnergy(left <= 0 ? 0 : (int)((long)energy * left / (left + 1)));
		}
	}

	/** Water on it: steam and a hiss, no flames. */
	private static void tickDoused(ServerLevel level, BlockPos pos, int left) {
		if(left % 5 == 0)
			level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 2, 0.3D, 0.1D, 0.3D, 0.02D);
		if(left % 30 == 0)
			level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 0.8F + level.random.nextFloat() * 0.4F);
	}

	/** Flames, fire around it, anything close set alight, and the batteries it touches catching. */
	private static void tickFlames(ServerLevel level, BlockPos pos, int left, List<BlockPos> spread) {
		double x = pos.getX() + 0.5D, y = pos.getY() + 1.0D, z = pos.getZ() + 0.5D;
		if(left % 2 == 0)
			level.sendParticles(ParticleTypes.FLAME, x, y - 0.4D, z, 2, 0.4D, 0.4D, 0.4D, 0.01D);
		if(left % 4 == 0)
			level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0.3D, 0.1D, 0.3D, 0.02D);
		if(left % 25 == 0)
			level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.0F, 0.8F);
		if(left % 20 != 0)
			return;
		for(Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(pos).inflate(1.0D)))
			entity.igniteForSeconds(4.0F);
		Direction direction = Direction.getRandom(level.random);
		BlockPos side = pos.relative(direction);
		if(level.getBlockState(side).isAir() && BaseFireBlock.canBePlacedAt(level, side, direction))
			level.setBlockAndUpdate(side, BaseFireBlock.getState(level, side));
		for(Direction d : Direction.values()) {
			BlockPos next = pos.relative(d);
			if(level.getBlockState(next).is(RadiocraftBlocks.LARGE_BATTERY.get()) && !isBurning(level, next) && level.random.nextInt(3) == 0)
				spread.add(next);
		}
	}

	/** Burned out: gone, and in a big enough group, an explosion. */
	private static void consume(ServerLevel level, BlockPos pos) {
		if(!level.getBlockState(pos).is(RadiocraftBlocks.LARGE_BATTERY.get()))
			return;
		Set<BlockPos> group = group(level, pos);
		level.removeBlock(pos, false);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 10, 0.3D, 0.3D, 0.3D, 0.03D);
		if(group.size() >= RadiocraftServerConfig.BATTERY_EXPLODE_GROUP.get()) {
			float power = (float)Math.min(group.size() * RadiocraftServerConfig.BATTERY_EXPLOSION_POWER_PER_BATTERY.get(), RadiocraftServerConfig.BATTERY_EXPLOSION_MAX_POWER.get());
			boolean fire = level.random.nextInt(4) == 0; // About a quarter of battery explosions start fires (proposal).
			level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, power, fire, Level.ExplosionInteraction.BLOCK);
			for(BlockPos survivor : group) // What the blast left of the group is alight.
				ignite(level, survivor);
		}
		else if(BaseFireBlock.canBePlacedAt(level, pos, Direction.UP))
			level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
	}

	/** How many large batteries touch each other in the group {@code pos} belongs to (itself included). */
	public static int groupSize(Level level, BlockPos pos) {
		return group(level, pos).size();
	}

	/** The large batteries touching each other in the group {@code pos} belongs to (itself included). */
	private static Set<BlockPos> group(Level level, BlockPos pos) {
		Block battery = RadiocraftBlocks.LARGE_BATTERY.get();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(pos);
		queue.add(pos);
		while(!queue.isEmpty() && seen.size() < MAX_GROUP) {
			BlockPos at = queue.poll();
			for(Direction direction : Direction.values()) {
				BlockPos next = at.relative(direction);
				if(level.getBlockState(next).is(battery) && seen.add(next))
					queue.add(next);
			}
		}
		return seen;
	}

	/**
	 * Lightning striking power wire near batteries sets the batteries that wire runs to alight (coax and antennas do
	 * not count: only power wire). Wire within {@code battery_lightning_wire_range} blocks of the strike.
	 */
	@SubscribeEvent
	public static void onEntityJoin(EntityJoinLevelEvent event) {
		if(!(event.getEntity() instanceof LightningBolt bolt) || !(event.getLevel() instanceof ServerLevel level) || !enabled())
			return;
		int range = RadiocraftServerConfig.BATTERY_LIGHTNING_WIRE_RANGE.get();
		if(range <= 0)
			return;
		BlockPos strike = bolt.blockPosition();
		Set<BlockPos> walked = new HashSet<>();
		for(BlockPos at : BlockPos.betweenClosed(strike.offset(-range, -range, -range), strike.offset(range, range, range))) {
			if(walked.contains(at) || !(level.getBlockState(at).getBlock() instanceof WireBlock wire) || !wire.isPower)
				continue;
			BlockPos wirePos = at.immutable();
			walked.add(wirePos);
			for(BENetworkObject object : WireUtils.getConnections(level, wirePos, o -> o instanceof BatteryNetworkObject, WireUtils.powerWires()).keySet())
				if(((BatteryNetworkObject)object).getStorage().getEnergyStored() > 0) // An empty battery has nothing to burn with.
					ignite(level, object.getPos());
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		BURNING.clear();
	}
}
