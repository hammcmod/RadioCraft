package com.arrl.radiocraft.compat.sable;

import com.arrl.radiocraft.common.entities.AntennaWire;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * What RadioCraft does when Sable assembles blocks into a sub-level (an airship, a boat). Sable carries blocks, block
 * entities and hanging entities across; an antenna wire is an entity strung between two blocks, so it is carried here,
 * when both blocks it hangs from went with the ship. A wire with one end left behind stays, and drops when its end
 * block is gone, as it would if the block were broken. Called by {@code MixinSableAssembly}; nothing here touches Sable.
 */
public final class SableAssembly {

	private SableAssembly() {}

	/**
	 * After Sable has moved {@code blocks} to their places in the sub-level's plot ({@code move} maps an old block
	 * position to its new one): moves every antenna wire whose two ends were among them. Returns how many it moved.
	 */
	public static int carryWires(ServerLevel level, UnaryOperator<BlockPos> move, Iterable<BlockPos> blocks) {
		Set<BlockPos> moved = new HashSet<>();
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		for(BlockPos block : blocks) {
			moved.add(block.immutable());
			minX = Math.min(minX, block.getX());
			minY = Math.min(minY, block.getY());
			minZ = Math.min(minZ, block.getZ());
			maxX = Math.max(maxX, block.getX());
			maxY = Math.max(maxY, block.getY());
			maxZ = Math.max(maxZ, block.getZ());
		}
		if(moved.isEmpty())
			return 0;
		AABB box = new AABB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
		int carried = 0;
		for(AntennaWire wire : level.getEntitiesOfClass(AntennaWire.class, box, w -> !w.isRemoved() && w.getWireHolder() == null)) {
			BlockPos start = wire.getStartPos(), end = wire.getEndPos();
			if(!moved.contains(start) || !moved.contains(end))
				continue;
			BlockPos newStart = move.apply(start);
			wire.setPos(newStart.getX() + 0.5D, newStart.getY() + 0.5D, newStart.getZ() + 0.5D);
			wire.setEndPos(move.apply(end));
			carried++;
		}
		return carried;
	}
}
