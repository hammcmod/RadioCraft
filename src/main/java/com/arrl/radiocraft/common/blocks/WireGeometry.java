package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;

/**
 * Where cable is drawn, as centre lines, worked out the way the client model places its pieces ({@link WireLook#parts},
 * {@link WireLook#affine}): for checking by eye-free means that cable meets cable across block faces and that the
 * strands of a bundle do not run through each other ({@code /rcdebug wires}, GameTests). Server safe: no models.
 */
public final class WireGeometry {

	private WireGeometry() {}

	/** A straight piece of a strand's centre line, world coordinates, and how wide the cable is (blocks). */
	public record Segment(WireBlock kind, Vec3 from, Vec3 to, double width) {}

	/** Every strand's centre line at {@code pos}. */
	public static List<Segment> at(BlockGetter level, BlockPos pos) {
		List<Segment> out = new ArrayList<>();
		List<BlockState> strands = Strand.statesAt(level, pos);
		WireLook.Lateral[] offsets = Strand.laterals(level, pos, strands);
		for(int i = 0; i < strands.size(); i++) {
			BlockState state = strands.get(i);
			WireBlock wire = (WireBlock)state.getBlock();
			WireLook.Lateral offset = strands.size() == 1 && level.getBlockState(pos).getBlock() instanceof WireBlock ? Strand.loneOffset(level, pos, state) : offsets[i];
			List<WireLook.Placed> parts = WireLook.parts(wire.look(level, pos, state), WireBlock.lookConnections(state));
			WireLook.Faces faces = wire.faces(level, pos, offset);
			boolean ceiling = WireBlock.isCeiling(state);
			double w = wire.width() / 16.0D, h = w / 2.0D;
			for(WireLook.Placed placed : parts)
				for(Vec3[] line : local(placed.part(), w, h)) {
					WireLook.Affine affine = WireLook.affine(placed, offset, faces, ceiling);
					out.add(new Segment(wire, world(pos, line[0], placed.dir(), ceiling, affine), world(pos, line[1], placed.dir(), ceiling, affine), w));
				}
		}
		return out;
	}

	/** A part's centre line(s) as its model draws it facing north, block units. */
	private static List<Vec3[]> local(WireLook.Part part, double w, double h) {
		double lo = 0.5D - h;
		return switch(part) {
			case MIDDLE -> line(new Vec3(0.5D, h, 0.5D), new Vec3(0.5D, h, 0.5D));
			case HORIZONTAL -> line(new Vec3(0.5D, h, lo), new Vec3(0.5D, h, 0.0D));
			case CORNER -> line(new Vec3(0.5D, h, 0.0D), new Vec3(0.5D, h, 0.0D));
			case VERTICAL -> line(new Vec3(0.5D, w, 0.5D), new Vec3(0.5D, 1.0D, 0.5D));
			case EDGE -> line(new Vec3(0.5D, h, 0.0D), new Vec3(0.5D, 1.0D, 0.0D)); // Starts on the floor stub: from its centre.
			case EDGE_FULL -> line(new Vec3(0.5D, 0.0D, 0.0D), new Vec3(0.5D, 1.0D, 0.0D));
			case CLIMB_OVERHANG -> line(new Vec3(0.5D, 1.0D + h, h), new Vec3(0.5D, 1.0D + h, -7.0D / 16.0D));
			case UP_OVERHANG -> line(new Vec3(0.5D, 1.0D + h, -h), new Vec3(0.5D, 1.0D + h, lo));
		};
	}

	private static List<Vec3[]> line(Vec3 from, Vec3 to) {
		List<Vec3[]> out = new ArrayList<>(1);
		out.add(new Vec3[] { from, to });
		return out;
	}

	/** Turned to its edge, flipped for a ceiling, moved by the strand's affine, then into the world at {@code pos}. */
	private static Vec3 world(BlockPos pos, Vec3 p, Direction dir, boolean ceiling, WireLook.Affine affine) {
		double x = p.x, z = p.z;
		switch(dir == null ? Direction.NORTH : dir) {
			case EAST -> { x = 1.0D - p.z; z = p.x; }
			case SOUTH -> { x = 1.0D - p.x; z = 1.0D - p.z; }
			case WEST -> { x = p.z; z = 1.0D - p.x; }
			default -> {}
		}
		double y = ceiling ? 1.0D - p.y : p.y;
		return new Vec3(pos.getX() + affine.x(x, y, z), pos.getY() + y, pos.getZ() + affine.z(x, y, z));
	}

	/** Every strand at {@code pos}: its offset, look, pieces and centre line (relative to the block), for /rcdebug. */
	public static List<String> describe(BlockGetter level, BlockPos pos) {
		List<String> out = new ArrayList<>();
		List<BlockState> strands = Strand.statesAt(level, pos);
		WireLook.Lateral[] offsets = Strand.laterals(level, pos, strands);
		if(level instanceof net.minecraft.world.level.Level real && level.getBlockEntity(pos) instanceof com.arrl.radiocraft.common.blockentities.WireBundleBlockEntity bundle)
			out.add("bundle flipX=" + bundle.flipX() + " flipZ=" + bundle.flipZ() + " bend=" + BundleOrientation.bend(real, pos, strands));
		for(int i = 0; i < strands.size(); i++) {
			BlockState state = strands.get(i);
			WireBlock wire = (WireBlock)state.getBlock();
			WireLook.Look look = wire.look(level, pos, state);
			out.add(String.format(Locale.ROOT, "%s offset %s links %s look hug=%s arrive=%s end=%s reach=%d/%d", name(wire), offsets[i],
					WireBlock.connected(state), look.hug(), look.arrive(), look.end(), look.hugReach(), look.arriveReach()));
			out.add("  parts " + WireLook.parts(look, WireBlock.lookConnections(state)) + " faces " + wire.faces(level, pos, offsets[i]));
		}
		for(Segment s : at(level, pos))
			out.add(String.format(Locale.ROOT, "  %s (%.2f %.2f %.2f) to (%.2f %.2f %.2f)", name(s.kind()),
					(s.from().x - pos.getX()) * 16, (s.from().y - pos.getY()) * 16, (s.from().z - pos.getZ()) * 16,
					(s.to().x - pos.getX()) * 16, (s.to().y - pos.getY()) * 16, (s.to().z - pos.getZ()) * 16));
		return out;
	}

	/**
	 * Problems in the box from {@code a} to {@code b}: cable of one kind that does not meet across a face where its
	 * strands link, and two strands whose centre lines come closer than their cables are wide (they run through each
	 * other), with where. Empty when all is well.
	 */
	public static List<String> check(BlockGetter level, BlockPos a, BlockPos b) {
		List<String> out = new ArrayList<>();
		for(BlockPos pos : BlockPos.betweenClosed(a, b)) {
			List<Segment> here = at(level, pos);
			if(here.isEmpty())
				continue;
			// Overlaps: segments of different strands in this block.
			for(int i = 0; i < here.size(); i++)
				for(int j = i + 1; j < here.size(); j++) {
					Segment s = here.get(i), t = here.get(j);
					if(s.kind() == t.kind())
						continue;
					double d = distance(s, t), room = (s.width() + t.width()) / 2.0D;
					if(d < room - 1e-4)
						out.add(String.format(Locale.ROOT, "%s: %s runs through %s (%.2f px apart, %.2f needed)", pos.toShortString(),
								name(s.kind()), name(t.kind()), d * 16.0D, room * 16.0D));
				}
			// Joins: each strand's ends on a face must meet the same kind's ends in the block across it.
			for(Direction dir : Direction.values()) {
				BlockPos next = pos.relative(dir);
				List<Segment> there = at(level, next);
				for(BlockState state : Strand.statesAt(level, pos)) {
					WireBlock kind = (WireBlock)state.getBlock();
					if(!state.getValue(WireBlock.PROPERTY_BY_DIRECTION.get(dir)) || kind.sameKindState(level, next) == null)
						continue;
					Vec3 mine = endOnFace(here, kind, pos, dir), theirs = endOnFace(there, ((WireBlock)kind.sameKindState(level, next).getBlock()), next, dir.getOpposite());
					if(mine == null || theirs == null)
						continue;
					double gap = mine.distanceTo(theirs);
					if(gap > 0.02D)
						out.add(String.format(Locale.ROOT, "%s %s: %s does not meet the next block's (%.2f px gap)", pos.toShortString(), dir.getName(), name(kind), gap * 16.0D));
				}
			}
		}
		return out;
	}

	/**
	 * How many pairs of strands run through each other in {@code blocks} (each pair counted once a block): what the
	 * bundle solver ({@link BundleOrientation}) keeps as low as it can.
	 */
	public static int crossings(BlockGetter level, Iterable<BlockPos> blocks) {
		int count = 0;
		for(BlockPos pos : blocks) {
			List<Segment> here = at(level, pos);
			Set<Long> pairs = new HashSet<>();
			for(int i = 0; i < here.size(); i++)
				for(int j = i + 1; j < here.size(); j++) {
					Segment s = here.get(i), t = here.get(j);
					if(s.kind() == t.kind())
						continue;
					if(distance(s, t) < (s.width() + t.width()) / 2.0D - 1e-4) {
						int a = System.identityHashCode(s.kind()), b = System.identityHashCode(t.kind());
						if(pairs.add(((long)Math.min(a, b) << 32) | (Math.max(a, b) & 0xFFFFFFFFL)))
							count++;
					}
				}
		}
		return count;
	}

	/** The end of {@code kind}'s centre line nearest the {@code dir} face of the block at {@code pos}, if one is on it. */
	private static Vec3 endOnFace(List<Segment> segments, WireBlock kind, BlockPos pos, Direction dir) {
		Vec3 best = null;
		double bestDist = 0.05D;
		for(Segment s : segments) {
			if(s.kind() != kind)
				continue;
			for(Vec3 p : new Vec3[] { s.from(), s.to() }) {
				double face = switch(dir) {
					case DOWN -> p.y - pos.getY();
					case UP -> pos.getY() + 1 - p.y;
					case NORTH -> p.z - pos.getZ();
					case SOUTH -> pos.getZ() + 1 - p.z;
					case WEST -> p.x - pos.getX();
					case EAST -> pos.getX() + 1 - p.x;
				};
				if(Math.abs(face) < bestDist) {
					bestDist = Math.abs(face);
					best = p;
				}
			}
		}
		return best;
	}

	private static String name(WireBlock kind) {
		return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(kind).getPath();
	}

	/** Shortest distance between two segments (a point counts as a segment of no length). */
	static double distance(Segment s, Segment t) {
		Vec3 p1 = s.from(), q1 = s.to(), p2 = t.from(), q2 = t.to();
		Vec3 d1 = q1.subtract(p1), d2 = q2.subtract(p2), r = p1.subtract(p2);
		double a = d1.dot(d1), e = d2.dot(d2), f = d2.dot(r), sc, tc;
		if(a < 1e-12 && e < 1e-12)
			return p1.distanceTo(p2);
		if(a < 1e-12) {
			sc = 0.0D;
			tc = clamp(f / e);
		}
		else {
			double c = d1.dot(r);
			if(e < 1e-12) {
				tc = 0.0D;
				sc = clamp(-c / a);
			}
			else {
				double bb = d1.dot(d2), denom = a * e - bb * bb;
				sc = denom > 1e-12 ? clamp((bb * f - c * e) / denom) : 0.0D;
				tc = (bb * sc + f) / e;
				if(tc < 0.0D) {
					tc = 0.0D;
					sc = clamp(-c / a);
				}
				else if(tc > 1.0D) {
					tc = 1.0D;
					sc = clamp((bb - c) / a);
				}
			}
		}
		return p1.add(d1.scale(sc)).distanceTo(p2.add(d2.scale(tc)));
	}

	private static double clamp(double v) {
		return Math.max(0.0D, Math.min(1.0D, v));
	}
}
