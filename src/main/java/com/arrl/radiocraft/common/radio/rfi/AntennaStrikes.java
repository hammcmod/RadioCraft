package com.arrl.radiocraft.common.radio.rfi;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.common.be_networks.BatteryFires;
import com.arrl.radiocraft.common.be_networks.WireUtils;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.blocks.radios.RadioBlock;
import com.arrl.radiocraft.common.capabilities.BENetworksCapability;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lightning striking an antenna (maintainer, 2026-10-24): the surge comes down the coax, burning out stretches of it
 * ({@code lightning_coax_burn_chance} per block, always the block at the antenna), blows every radio on the run
 * (SWR protection does not help), and now and then ({@code lightning_battery_ignite_chance}) sets the batteries those
 * radios run on alight. A lightning arrestor on the coax will stop it, once there is one.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public final class AntennaStrikes {

	/** Blocks from the strike an antenna counts as hit (the bolt lands on top of it). */
	private static final double HIT_RANGE = 2.5D;
	/** Coax blocks a surge walk follows before stopping. */
	private static final int MAX_COAX = 256;

	private AntennaStrikes() {}

	@SubscribeEvent
	public static void onEntityJoin(EntityJoinLevelEvent event) {
		if(event.getEntity() instanceof LightningBolt bolt && event.getLevel() instanceof ServerLevel level)
			strike(level, bolt.position());
	}

	/** A strike at {@code at}: every antenna it hit takes the surge. */
	public static void strike(ServerLevel level, Vec3 at) {
		if(!RadiocraftServerConfig.LIGHTNING_ANTENNA_DAMAGE.get())
			return;
		for(IAntenna antenna : List.copyOf(AntennaNetworkManager.getAllAntennas())) {
			IAntenna.AntennaPos pos = antenna.getAntennaPos();
			if(pos == null || pos.level() != level || antenna.getPlayer() != null)
				continue;
			if(com.arrl.radiocraft.compat.sable.Sublevels.worldCenter(level, pos.position()).distanceTo(at) <= HIT_RANGE && damages(antenna.getType().lightningDamageFactor(), level.random.nextDouble()))
				surge(level, pos.position());
		}
	}

	/** Whether a strike does damage, for an antenna with {@code factor} (see {@link IAntenna#getType()}) and a roll of 0 to 1. */
	public static boolean damages(double factor, double roll) {
		return roll < factor;
	}

	/** The antenna at {@code antennaPos} was hit. */
	public static void surge(ServerLevel level, BlockPos antennaPos) {
		List<RadioNetworkObject> radios = BENetworksCapability.get(level).getObject(antennaPos) instanceof AntennaNetworkObject antenna ? antenna.getRadios() : List.of();
		boolean ignite = level.random.nextDouble() < RadiocraftServerConfig.LIGHTNING_BATTERY_IGNITE_CHANCE.get();
		for(RadioNetworkObject radio : radios) {
			if(level.getBlockEntity(radio.getPos()) instanceof RadioBlockEntity be && be.getBlockState().hasProperty(RadioBlock.BLOWN))
				be.blow(level);
			if(ignite) {
				for(BENetwork network : radio.getNetworks().values()) {
					if(!(network instanceof PowerBENetwork))
						continue;
					for(BENetworkObject object : List.copyOf(network.getNetworkObjects())) {
						if(object instanceof BatteryNetworkObject battery && battery.getStorage().getEnergyStored() > 0)
							BatteryFires.ignite(level, object.getPos());
					}
				}
			}
		}
		burnCoax(level, antennaPos);
	}

	/** Burns out coax on the run from the antenna: the block at the antenna always, the rest by chance. */
	private static void burnCoax(ServerLevel level, BlockPos antennaPos) {
		Set<WireBlock> coax = Set.of(WireUtils.coaxWires());
		double chance = RadiocraftServerConfig.LIGHTNING_COAX_BURN_CHANCE.get();
		Set<BlockPos> first = WireUtils.wireLinksOf(level, antennaPos, coax).keySet();
		Set<BlockPos> seen = new LinkedHashSet<>(first);
		ArrayDeque<BlockPos> queue = new ArrayDeque<>(first);
		while(!queue.isEmpty() && seen.size() < MAX_COAX) {
			BlockPos at = queue.poll();
			for(BlockState state : com.arrl.radiocraft.common.blocks.Strand.statesAt(level, at)) { // A coax block, or a bundle's coax strands.
				WireBlock wire = (WireBlock)state.getBlock();
				if(!coax.contains(wire))
					continue;
				for(BlockPos next : wire.getLinks(level, at, state).keySet()) {
					if(WireUtils.hasCoax(level, next) && seen.add(next))
						queue.add(next);
				}
			}
		}
		List<BlockPos> burnt = new ArrayList<>();
		for(BlockPos pos : seen) {
			if(first.contains(pos) || level.random.nextDouble() < chance)
				burnt.add(pos);
		}
		Set<BlockPos> done = new HashSet<>();
		for(BlockPos pos : burnt) {
			if(!done.add(pos) || !WireUtils.hasCoax(level, pos))
				continue;
			if(level.getBlockState(pos).getBlock() instanceof com.arrl.radiocraft.common.blocks.WireBundleBlock) { // The coax burns out of the bundle; the power wire stays.
				for(BlockState state : com.arrl.radiocraft.common.blocks.Strand.statesAt(level, pos))
					if(coax.contains((WireBlock)state.getBlock()))
						com.arrl.radiocraft.common.blocks.WireBundleBlock.remove(level, pos, (WireBlock)state.getBlock());
			}
			else
				level.removeBlock(pos, false);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D, 4, 0.2D, 0.1D, 0.2D, 0.02D);
			level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.2F);
		}
	}
}
