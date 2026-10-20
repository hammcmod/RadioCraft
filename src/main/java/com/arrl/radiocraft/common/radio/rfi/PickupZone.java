package com.arrl.radiocraft.common.radio.rfi;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;

import java.util.List;

/**
 * Where one station picks up redstone interference: the feedline (the radio block and the coax between it and the
 * antenna) and the antenna itself (its centre block and the wire runs). Redstone switching within
 * {@link #FEEDLINE_RADIUS} blocks of a feedline point or {@link #ANTENNA_RADIUS} blocks of an antenna point adds noise,
 * fading linearly to nothing at the edge. The zone is immutable; the radio rebuilds it when its antenna or power changes.
 *
 * @param radioPos The radio that hears the noise.
 * @param feedline Radio block plus coax positions.
 * @param antenna Antenna centre plus points sampled along its wires.
 * @param min Lowest corner of the area any pickup can happen in (points padded by the radii).
 * @param max Highest corner of that area.
 */
public record PickupZone(BlockPos radioPos, List<BlockPos> feedline, List<BlockPos> antenna, BlockPos min, BlockPos max) {

	public static final int FEEDLINE_RADIUS = 4;
	public static final int ANTENNA_RADIUS = 8;

	public static PickupZone of(BlockPos radioPos, List<BlockPos> feedline, List<BlockPos> antenna) {
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		for(BlockPos p : feedline) {
			minX = Math.min(minX, p.getX() - FEEDLINE_RADIUS); minY = Math.min(minY, p.getY() - FEEDLINE_RADIUS); minZ = Math.min(minZ, p.getZ() - FEEDLINE_RADIUS);
			maxX = Math.max(maxX, p.getX() + FEEDLINE_RADIUS); maxY = Math.max(maxY, p.getY() + FEEDLINE_RADIUS); maxZ = Math.max(maxZ, p.getZ() + FEEDLINE_RADIUS);
		}
		for(BlockPos p : antenna) {
			minX = Math.min(minX, p.getX() - ANTENNA_RADIUS); minY = Math.min(minY, p.getY() - ANTENNA_RADIUS); minZ = Math.min(minZ, p.getZ() - ANTENNA_RADIUS);
			maxX = Math.max(maxX, p.getX() + ANTENNA_RADIUS); maxY = Math.max(maxY, p.getY() + ANTENNA_RADIUS); maxZ = Math.max(maxZ, p.getZ() + ANTENNA_RADIUS);
		}
		return new PickupZone(radioPos, List.copyOf(feedline), List.copyOf(antenna), new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
	}

	/** True if {@code pos} is inside the padded bounding box, the cheap test done before any distance maths. */
	public boolean mayAffect(BlockPos pos) {
		return pos.getX() >= min.getX() && pos.getX() <= max.getX()
				&& pos.getY() >= min.getY() && pos.getY() <= max.getY()
				&& pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
	}

	/**
	 * How strongly a switch at {@code pos} couples into this station, 0 to 1: 1 right on the wire, falling linearly to
	 * 0 at the radius. The feedline and antenna are checked separately and the stronger coupling wins.
	 */
	public double coupling(BlockPos pos) {
		if(!mayAffect(pos))
			return 0.0D;
		double best = coupling(pos, feedline, FEEDLINE_RADIUS);
		if(best < 1.0D)
			best = Math.max(best, coupling(pos, antenna, ANTENNA_RADIUS));
		return best;
	}

	private static double coupling(BlockPos pos, List<BlockPos> points, int radius) {
		double nearestSq = Double.MAX_VALUE;
		for(BlockPos p : points)
			nearestSq = Math.min(nearestSq, pos.distSqr(p));
		if(nearestSq >= (double)radius * radius)
			return 0.0D;
		return 1.0D - Math.sqrt(nearestSq) / radius;
	}

	/** Every chunk the padded bounding box touches, as {@link ChunkPos#asLong} keys. */
	public long[] chunks() {
		int minCX = min.getX() >> 4, maxCX = max.getX() >> 4;
		int minCZ = min.getZ() >> 4, maxCZ = max.getZ() >> 4;
		long[] out = new long[(maxCX - minCX + 1) * (maxCZ - minCZ + 1)];
		int i = 0;
		for(int cx = minCX; cx <= maxCX; cx++)
			for(int cz = minCZ; cz <= maxCZ; cz++)
				out[i++] = ChunkPos.asLong(cx, cz);
		return out;
	}

	/**
	 * Points one block apart along the straight line from {@code from} to {@code to}, both ends included, as sampled
	 * along an antenna wire.
	 */
	public static void sampleLine(BlockPos from, BlockPos to, List<BlockPos> out) {
		Vec3i d = to.subtract(from);
		int steps = Math.max(Math.abs(d.getX()), Math.max(Math.abs(d.getY()), Math.abs(d.getZ())));
		if(steps == 0) {
			out.add(from);
			return;
		}
		for(int i = 0; i <= steps; i++) {
			double t = (double)i / steps;
			out.add(new BlockPos(from.getX() + (int)Math.round(d.getX() * t), from.getY() + (int)Math.round(d.getY() * t), from.getZ() + (int)Math.round(d.getZ() * t)));
		}
	}
}
