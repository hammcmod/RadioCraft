package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Keeps wire climbs ({@link WireBlock#CLIMB_BY_DIRECTION}) in step with the world. A climb depends on blocks that are
 * not direct neighbours of the wire (the block on top of the wall it climbs), and the wire on top needs a stub towards
 * a climber that is below-diagonal, so vanilla shape updates never reach them. Whenever any block notifies its
 * neighbours of a change, every wire in the 3x3x3 cube around it is asked to {@link WireBlock#refresh} (a bundle,
 * {@link WireBundleBlock#refresh} for each of its strands), which is a no-op unless its state really changed, so this
 * converges in one or two steps and costs a few block lookups.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public class WireClimbUpdater {

	@SubscribeEvent
	public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
		if(!(event.getLevel() instanceof Level level) || level.isClientSide)
			return;
		BlockPos pos = event.getPos();
		for(int dx = -1; dx <= 1; dx++)
			for(int dy = -1; dy <= 1; dy++)
				for(int dz = -1; dz <= 1; dz++) {
					if(dx == 0 && dy == 0 && dz == 0)
						continue;
					BlockPos near = pos.offset(dx, dy, dz);
					if(level.getBlockState(near).getBlock() instanceof WireBlock wire)
						wire.refresh(level, near);
					else if(level.getBlockState(near).getBlock() instanceof WireBundleBlock bundle)
						bundle.refresh(level, near);
				}
	}
}
