package com.arrl.radiocraft.common.radio.microwave;

import com.arrl.radiocraft.common.radio.terrain.TerrainMath;
import com.arrl.radiocraft.common.radio.terrain.TerrainPropagation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Whether a microwave beam gets through, block by block. The HF terrain atlas ({@link TerrainPropagation#atlas}) is a
 * 4x4-column average meant for hills a long way off; between two dishes a dozen blocks apart on short poles it called
 * a one-block step in the ground, or a tree trunk in the averaged cell, "ground within a block of the beam" and no
 * link was ever possible (reported 2026-10-27). So the beam is walked through the loaded world instead: anything with a
 * collision shape on the straight line blocks it (walls, trunks and leaves; wires and air do not), ends excepted as
 * before. Where the world is not loaded the atlas still answers.
 */
public final class MicrowavePaths {

	/** How far along the beam between samples, blocks. */
	private static final double STEP = 0.5D;

	private MicrowavePaths() {}

	/** The {@link MicrowaveMath.Path} for {@code level}: exact where loaded, the atlas elsewhere. */
	public static MicrowaveMath.Path in(Level level) {
		TerrainMath.HeightSource atlas = TerrainPropagation.atlas(level);
		return (a, b) -> clear(level, atlas, a, b);
	}

	/** True if nothing solid lies on the straight line between {@code a} and {@code b}, {@link MicrowaveMath#END_CLEARANCE} at each end excepted. */
	public static boolean clear(Level level, @Nullable TerrainMath.HeightSource heights, MicrowaveMath.End a, MicrowaveMath.End b) {
		double dx = b.x() - a.x(), dy = b.y() - a.y(), dz = b.z() - a.z();
		double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if(distance <= 2.0D * MicrowaveMath.END_CLEARANCE)
			return true;
		int steps = (int)Math.ceil(distance / STEP);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for(int i = 0; i <= steps; i++) {
			double d = distance * i / steps;
			if(d < MicrowaveMath.END_CLEARANCE || d > distance - MicrowaveMath.END_CLEARANCE)
				continue;
			double f = d / distance;
			double x = a.x() + dx * f, y = a.y() + dy * f, z = a.z() + dz * f;
			pos.set(Math.floor(x), Math.floor(y), Math.floor(z));
			if(!level.isLoaded(pos)) {
				if(heights != null) { // Unexplored: the atlas, as it always was.
					int h = heights.surface(pos.getX(), pos.getZ());
					if(h != TerrainMath.UNKNOWN && h > y - MicrowaveMath.CLEARANCE)
						return false;
				}
				continue;
			}
			BlockState state = level.getBlockState(pos);
			if(!state.isAir() && !state.getCollisionShape(level, pos).isEmpty())
				return false;
		}
		return true;
	}
}
