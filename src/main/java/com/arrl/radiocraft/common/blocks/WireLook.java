package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * How a wire block looks: which cable pieces it draws, and where. Pure logic with no world access, so the hitbox
 * ({@link WireBlock#getShape}), the client model ({@code WireBakedModel}) and the unit tests all use the same rule.
 * {@link WireBlock#look} works out the {@link Look} from the world.
 * <p>
 * The rule: cable already lies on the edges in S = {arrive, hug, and the opposite wall for a second climb}. The
 * "ports" the cable must reach are the connected neighbours (at their edges), the top of the block when the run up
 * goes through the middle, and the bottom when the run from below comes up the middle. The middle piece is drawn only
 * when the cable has to pass through the middle: a port not on an edge in S, two different edges in S, no edge cable
 * at all, or a dead end (a nub so the block is visible). A stub joins the middle to every edge that has cable or a
 * neighbour. A run arriving at an edge and leaving sideways on that same edge, with nothing else to reach, is just a
 * corner cube on that edge.
 */
public final class WireLook {

	private WireLook() {}

	/** How the run up the hugged edge ends, for the pieces drawn beyond the block. */
	public enum End {
		/** Nothing beyond the block: a wire above continues it, or there is no run up. */
		NONE,
		/** A climb up the hugged wall to the wire on top of it; that wire draws the arrival. */
		CLIMB_WIRE,
		/** A climb up the hugged wall to a device on top of it; we draw the cable over the wall's top to its middle. */
		CLIMB_DEVICE,
		/** The run up the hugged edge ends at a device directly above; we draw the cable in to its middle. */
		UP_DEVICE;

		public boolean isClimb() {
			return this == CLIMB_WIRE || this == CLIMB_DEVICE;
		}
	}

	/**
	 * The vertical side of a wire's look. {@code hug}: the edge the run up (or the climb) follows, null for the middle
	 * or no run up. {@code arrive}: the edge a run from below comes in on (from the wire below or a climber), null for
	 * the middle or none. {@code back}: a second climb up the wall opposite {@code hug} (a one-wide trench).
	 */
	public record Look(@Nullable Direction hug, @Nullable Direction arrive, End end, boolean back) {
		public static final Look NONE = new Look(null, null, End.NONE, false);
	}

	/** A piece of cable: the part model suffix ("edge" -> "wire_edge") and whether it is turned per edge. */
	public enum Part {
		MIDDLE("middle", false), HORIZONTAL("horizontal", true), CORNER("corner", true), VERTICAL("vertical", false),
		EDGE("edge", true), EDGE_FULL("edge_full", true), CLIMB_OVERHANG("climb_overhang", true), UP_OVERHANG("up_overhang", true);

		public final String suffix;
		public final boolean rotates;

		Part(String suffix, boolean rotates) {
			this.suffix = suffix;
			this.rotates = rotates;
		}
	}

	/** A piece placed on an edge ({@code dir}, north being the model's own orientation), or unturned (null). */
	public record Placed(Part part, @Nullable Direction dir) {}

	/**
	 * The pieces to draw for a wire with these connections and this look, in a fixed order (so equal looks give equal
	 * lists, which the model and the shape cache key on).
	 */
	public static List<Placed> parts(Look look, Set<Direction> connected) {
		Direction hug = look.hug(), arrive = look.arrive();
		Direction back = hug != null && look.back() ? hug.getOpposite() : null;
		boolean up = connected.contains(Direction.UP), down = connected.contains(Direction.DOWN);
		Set<Direction> edgeCable = EnumSet.noneOf(Direction.class);
		if(hug != null)
			edgeCable.add(hug);
		if(arrive != null)
			edgeCable.add(arrive);
		if(back != null)
			edgeCable.add(back);

		boolean portOffEdge = (up && hug == null) || (down && arrive == null);
		boolean anyPort = portOffEdge;
		for(Direction dir : Direction.Plane.HORIZONTAL) {
			if(connected.contains(dir)) {
				anyPort = true;
				portOffEdge |= !edgeCable.contains(dir);
			}
		}
		// The cable passes straight through along an edge when it arrives there and goes on up (or climbs).
		boolean through = arrive != null && (up || look.end().isClimb());
		boolean middle = portOffEdge || edgeCable.size() != 1 || (!anyPort && !through);

		List<Placed> out = new ArrayList<>(6);
		if(middle)
			out.add(new Placed(Part.MIDDLE, null));
		for(Direction dir : Direction.Plane.HORIZONTAL) {
			boolean link = connected.contains(dir);
			if(middle && (link || edgeCable.contains(dir)))
				out.add(new Placed(Part.HORIZONTAL, dir));
			else if(link && arrive == dir && hug == null)
				out.add(new Placed(Part.CORNER, dir));
		}
		if(hug != null) {
			out.add(new Placed(arrive == hug ? Part.EDGE_FULL : Part.EDGE, hug));
			if(look.end() == End.CLIMB_DEVICE)
				out.add(new Placed(Part.CLIMB_OVERHANG, hug));
			if(look.end() == End.UP_DEVICE)
				out.add(new Placed(Part.UP_OVERHANG, hug));
			if(back != null)
				out.add(new Placed(arrive == back ? Part.EDGE_FULL : Part.EDGE, back));
		}
		else if(up)
			out.add(new Placed(Part.VERTICAL, null));
		if(out.isEmpty())
			out.add(new Placed(Part.MIDDLE, null));
		return List.copyOf(out);
	}

	/** Degrees the part model is turned about Y for an edge, north being the model's own orientation. */
	public static int rotation(@Nullable Direction dir) {
		if(dir == null)
			return 0;
		return switch(dir) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}
}
