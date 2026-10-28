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

	/**
	 * Where a piece sits for a strand drawn {@code offset} sixteenths off the middle of the block (a cable in a bundle,
	 * see {@link com.arrl.radiocraft.common.blocks.Strand#offset}), as a map of block-local x and z: {@code x' = sx * x +
	 * tx + kxy * y + kxz * z}, {@code z' = sz * z + tz + kzx * x + kzy * y}, y untouched. The middle and the run up the
	 * middle move {@code offset} along both axes; a horizontal stub moves sideways and is stretched so it still reaches
	 * the block face it leaves by while its inner end meets the moved middle; pieces on an edge (the run up a wall and
	 * its corner) slide along that wall and stay against it. The cross terms are a shear: a stub slants from this
	 * strand's offset at the middle to where it meets the next block's cable at the face ({@link Faces}), so a strand
	 * leaving a bundle for a lone wire eases over instead of stepping. Applied after the piece is turned to its edge, so
	 * the same map serves the hitbox and the quads.
	 */
	public record Affine(double sx, double tx, double kxy, double kxz, double sz, double tz, double kzx, double kzy) {
		public static final Affine NONE = new Affine(1.0D, 0.0D, 0.0D, 0.0D, 1.0D, 0.0D, 0.0D, 0.0D);

		/** A plain move and stretch, no shear. */
		public static Affine of(double sx, double tx, double sz, double tz) {
			return new Affine(sx, tx, 0.0D, 0.0D, sz, tz, 0.0D, 0.0D);
		}

		public double x(double x, double y, double z) {
			return sx * x + tx + kxy * y + kxz * z;
		}

		public double z(double x, double y, double z) {
			return sz * z + tz + kzx * x + kzy * y;
		}

		/** For a piece with no shear (the tests): x and z alone. */
		public double x(double x) {
			return x(x, 0.0D, 0.0D);
		}

		public double z(double z) {
			return z(0.0D, 0.0D, z);
		}
	}

	/**
	 * Where this strand's cable meets the cable in each of the six neighbouring blocks, in sixteenths off the middle,
	 * laterally: halfway between this strand's offset and the neighbour's (a lone wire or a device is at 0), so both
	 * blocks draw the crossing at the same place. World directions, whatever way up the wire is.
	 */
	public record Faces(double down, double up, double north, double south, double west, double east) {
		/** Every face at {@code offset}: a strand whose neighbours sit where it does (and the lone wire, at 0). */
		public static Faces all(double offset) {
			return new Faces(offset, offset, offset, offset, offset, offset);
		}

		public double at(Direction dir) {
			return switch(dir) {
				case DOWN -> down;
				case UP -> up;
				case NORTH -> north;
				case SOUTH -> south;
				case WEST -> west;
				case EAST -> east;
			};
		}
	}

	/** {@link #affine(Placed, double, Faces, boolean)} for a strand whose neighbours all sit at its own offset. */
	public static Affine affine(Placed placed, int offset) {
		return affine(placed, offset, Faces.all(offset), false);
	}

	/**
	 * See {@link Affine}; a stub is 7 pixels from the face to the middle. {@code own} is the strand's offset in this
	 * block, {@code faces} where it meets each neighbour, {@code ceiling} whether the wire hangs (its pieces are drawn
	 * upside down, so a run "up" an edge goes down the block in world y).
	 */
	public static Affine affine(Placed placed, double own, Faces faces, boolean ceiling) {
		Direction dir = placed.dir();
		double o = own / 16.0D;
		Direction top = ceiling ? Direction.DOWN : Direction.UP, bottom = top.getOpposite(); // World ends of a run "up".
		switch(placed.part()) {
			case MIDDLE -> {
				return own == 0.0D ? Affine.NONE : Affine.of(1.0D, o, 1.0D, o);
			}
			case VERTICAL -> { // From the middle (our offset) to the block above (its meeting point), along y.
				return slantY(o, faces.at(top) / 16.0D, ceiling, o, faces.at(top) / 16.0D, ceiling);
			}
			case HORIZONTAL, UP_OVERHANG -> {
				double face = faces.at(dir) / 16.0D; // Lateral offset at the block face; o at the inner end.
				if(placed.part() == Part.UP_OVERHANG)
					face = o = faces.at(top) / 16.0D; // Carries on from the top of the edge run at the meeting point above.
				return stub(dir, o, face, own);
			}
			case CORNER -> { // The little cube at the edge: at the meeting point with the neighbour it turns to.
				double face = faces.at(dir) / 16.0D;
				return dir.getAxis() == Direction.Axis.Z ? Affine.of(1.0D, face, 1.0D, 0.0D) : Affine.of(1.0D, 0.0D, 1.0D, face);
			}
			case EDGE -> { // Up the wall: our offset at the foot, the meeting point with the block above at the top.
				return alongWall(dir, o, faces.at(top) / 16.0D, ceiling);
			}
			case EDGE_FULL -> { // Through from the block below to the block above.
				return alongWall(dir, faces.at(bottom) / 16.0D, faces.at(top) / 16.0D, ceiling);
			}
			case CLIMB_OVERHANG -> { // Over the wall's top: at the meeting point with whatever is on top.
				double face = faces.at(top) / 16.0D;
				return dir.getAxis() == Direction.Axis.Z ? Affine.of(1.0D, face, 1.0D, 0.0D) : Affine.of(1.0D, 0.0D, 1.0D, face);
			}
		}
		return Affine.NONE;
	}

	/**
	 * A stub towards {@code dir}: stretched along its axis so its face end stays on the face while its inner end meets
	 * the middle moved by {@code own} sixteenths, and sheared sideways from {@code inner} (block units) at the middle to
	 * {@code face} at the block face.
	 */
	private static Affine stub(Direction dir, double inner, double face, double own) {
		double stretchIn = (7.0D + own) / 7.0D, stretchOut = (7.0D - own) / 7.0D; // The stub is 7 px from face to middle.
		// Sideways position as a function of the position along the stub, a line through (face end, face) and (inner end, inner).
		return switch(dir) {
			case NORTH -> new Affine(1.0D, face, 0.0D, (inner - face) / (7.0D / 16.0D), stretchIn, 0.0D, 0.0D, 0.0D); // z 0 at the face, 7/16 at the middle.
			case SOUTH -> new Affine(1.0D, face + (inner - face) / (7.0D / 16.0D), 0.0D, -(inner - face) / (7.0D / 16.0D), stretchOut, 1.0D - stretchOut, 0.0D, 0.0D); // z 1 at the face, 9/16 at the middle.
			case EAST -> new Affine(stretchOut, 1.0D - stretchOut, 0.0D, 0.0D, 1.0D, face + (inner - face) / (7.0D / 16.0D), -(inner - face) / (7.0D / 16.0D), 0.0D); // x 1 at the face, 9/16 at the middle.
			case WEST -> new Affine(stretchIn, 0.0D, 0.0D, 0.0D, 1.0D, face, (inner - face) / (7.0D / 16.0D), 0.0D); // x 0 at the face.
			default -> Affine.NONE;
		};
	}

	/** A run up the wall on edge {@code dir}, sliding along the wall from {@code foot} at the floor end to {@code head} at the far end. */
	private static Affine alongWall(Direction dir, double foot, double head, boolean ceiling) {
		double y0 = ceiling ? head : foot, y1 = ceiling ? foot : head; // Lateral offset at world y 0 and y 1.
		return dir.getAxis() == Direction.Axis.Z ? new Affine(1.0D, y0, y1 - y0, 0.0D, 1.0D, 0.0D, 0.0D, 0.0D)
				: new Affine(1.0D, 0.0D, 0.0D, 0.0D, 1.0D, y0, 0.0D, y1 - y0);
	}

	/** The run up the middle, moved by ({@code x0}, {@code z0}) at the floor end and ({@code x1}, {@code z1}) at the far end. */
	private static Affine slantY(double x0, double x1, boolean ceiling, double z0, double z1, boolean ceilingZ) {
		double ax = ceiling ? x1 : x0, bx = ceiling ? x0 : x1, az = ceilingZ ? z1 : z0, bz = ceilingZ ? z0 : z1;
		return new Affine(1.0D, ax, bx - ax, 0.0D, 1.0D, az, 0.0D, bz - az);
	}

	/** Room a bundle's strands may take up side by side, pixels: a pixel of margin either side of the block. */
	public static final double BUNDLE_ROOM = 14.0D;
	/** Space between strands in a bundle, pixels, given up only when they would not fit otherwise. */
	public static final double BUNDLE_GAP = 1.0D;

	/**
	 * Where strands of the given widths (pixels) sit side by side: the centre of each, in pixels off the middle of the
	 * block, laid left to right with {@link #BUNDLE_GAP} between them and the whole centred. If that is wider than
	 * {@link #BUNDLE_ROOM} the gaps shrink (to nothing, at worst) so the strands still fit.
	 */
	public static double[] layout(double[] widths) {
		int n = widths.length;
		double total = 0.0D;
		for(double w : widths)
			total += w;
		double gap = n <= 1 ? 0.0D : Math.max(0.0D, Math.min(BUNDLE_GAP, (BUNDLE_ROOM - total) / (n - 1)));
		double span = total + gap * Math.max(0, n - 1);
		double[] out = new double[n];
		double at = -span / 2.0D;
		for(int i = 0; i < n; i++) {
			out[i] = at + widths[i] / 2.0D;
			at += widths[i] + gap;
		}
		return out;
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
