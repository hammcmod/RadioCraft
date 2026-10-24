package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.antenna.AntennaTypes;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.WireUtils;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.capabilities.BENetworksCapability;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Lightning rods as antennas, and antennas as (weaker) lightning rods (maintainer, 2026-10-24).
 * <p>
 * A vanilla lightning rod has no block entity to make its network object, so one is made here: when a coax walk
 * reaches a rod (a radio loading, coax being laid), and when a rod is placed. It goes again when the rod does.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public final class LightningRodAntennas {

	private LightningRodAntennas() {}

	/** The antenna object for the lightning rod at {@code pos}, made if missing; null when there is no rod there. */
	public static BENetworkObject ensure(Level level, BlockPos pos) {
		if(level.isClientSide || !level.getBlockState(pos).is(Blocks.LIGHTNING_ROD))
			return null;
		IBENetworks cap = BENetworksCapability.get(level);
		BENetworkObject existing = cap.getObject(pos);
		if(existing != null)
			return existing;
		AntennaNetworkObject object = new AntennaNetworkObject(level, pos.immutable(), AntennaNetworkManager.VHF_ID);
		if(AntennaTypes.match(level, pos) instanceof StaticAntenna<?> antenna)
			object.setAntenna(antenna);
		cap.setObject(pos.immutable(), object);
		return object;
	}

	/** A rod placed: onto any coax touching it. A rod gone: off the air and off the coax. */
	@SubscribeEvent
	public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
		if(!(event.getLevel() instanceof ServerLevel level))
			return;
		BlockPos pos = event.getPos();
		BlockState state = event.getState();
		IBENetworks cap = BENetworksCapability.get(level);
		BENetworkObject object = cap.getObject(pos);
		if(state.is(Blocks.LIGHTNING_ROD)) {
			if(object == null) {
				ensure(level, pos);
				WireUtils.connectToWires(level, pos);
			}
		}
		else if(object instanceof AntennaNetworkObject && level.getBlockEntity(pos) == null) { // The rod's: no block entity made it.
			object.clearNetworks();
			cap.removeObject(pos);
		}
	}

	/**
	 * Where lightning lands instead of {@code target}, which no lightning rod claimed: now and then
	 * ({@code antenna_lightning_chance}) the highest antenna within {@code antenna_lightning_range} blocks under open
	 * sky, so antennas draw strikes like a lightning rod, but less often and from less far. {@code target} when none.
	 */
	public static BlockPos redirect(ServerLevel level, BlockPos target, RandomSource random) {
		double chance = RadiocraftServerConfig.ANTENNA_LIGHTNING_CHANCE.get();
		if(chance <= 0.0D || random.nextDouble() >= chance)
			return target;
		int range = RadiocraftServerConfig.ANTENNA_LIGHTNING_RANGE.get();
		BlockPos best = null;
		for(IAntenna antenna : AntennaNetworkManager.getAllAntennas()) {
			IAntenna.AntennaPos at = antenna.getAntennaPos();
			if(at == null || at.level() != level || antenna.getPlayer() != null)
				continue;
			BlockPos pos = at.position();
			int dx = pos.getX() - target.getX(), dz = pos.getZ() - target.getZ();
			if(dx * dx + dz * dz > range * range || !level.isLoaded(pos))
				continue;
			if((best == null || pos.getY() > best.getY()) && level.isRainingAt(pos.above()))
				best = pos;
		}
		return best == null ? target : best.above();
	}
}
