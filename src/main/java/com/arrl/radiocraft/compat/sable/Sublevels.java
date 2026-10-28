package com.arrl.radiocraft.compat.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * Sable support (https://github.com/ryanhcode/sable): blocks on a moving sub-level (an airship, a boat) are stored in a
 * plot far out in the world and drawn where the sub-level really is. Everything that compares or reports where a radio
 * is (range, skip, terrain, weather, bearings, voice audio) asks here for the real place.
 *
 * <p>Built on Sable Companion, which is bundled in the mod: without Sable installed every method returns what it was
 * given. Server or client thread only (Sable's plot grid is not thread safe); hand a {@link Frame} to other threads.</p>
 */
public final class Sublevels {

	private Sublevels() {}

	/** The sub-level {@code pos} is on, or null for an ordinary block (and always without Sable). */
	@Nullable
	public static SubLevelAccess at(@Nullable Level level, BlockPos pos) {
		return level == null ? null : SableCompanion.INSTANCE.getContaining(level, pos);
	}

	/** Whether {@code pos} is a block on a sub-level. */
	public static boolean onSublevel(@Nullable Level level, BlockPos pos) {
		return at(level, pos) != null;
	}

	/** Where the block at {@code pos} really is in the world: {@code pos} itself unless it is on a sub-level. */
	public static BlockPos world(@Nullable Level level, BlockPos pos) {
		SubLevelAccess sub = at(level, pos);
		return sub == null ? pos : BlockPos.containing(sub.logicalPose().transformPosition(Vec3.atCenterOf(pos)));
	}

	/** The real place of the centre of the block at {@code pos}. */
	public static Vec3 worldCenter(@Nullable Level level, BlockPos pos) {
		return worldPoint(level, pos, Vec3.atCenterOf(pos));
	}

	/** The real place of {@code point}, a spot on or near the block at {@code block} (which picks the sub-level). */
	public static Vec3 worldPoint(@Nullable Level level, BlockPos block, Vec3 point) {
		SubLevelAccess sub = at(level, block);
		return sub == null ? point : sub.logicalPose().transformPosition(point);
	}

	/** Squared distance between two blocks where they really are (either or both may be on a sub-level). */
	public static double distSqr(@Nullable Level level, BlockPos a, BlockPos b) {
		return world(level, a).distSqr(world(level, b));
	}

	/**
	 * {@code other} as seen from the sub-level {@code anchor} is on: its real place, carried into that sub-level's own
	 * block grid, so a direction worked out from {@code anchor} (a Yagi's facing, a loop's broadside) turns with the
	 * ship. {@code other} as it is when neither block is on a sub-level.
	 */
	public static BlockPos inFrameOf(@Nullable Level level, BlockPos anchor, BlockPos other) {
		SubLevelAccess home = at(level, anchor);
		SubLevelAccess away = at(level, other);
		if(home == null && away == null)
			return other;
		Vec3 real = away == null ? Vec3.atCenterOf(other) : away.logicalPose().transformPosition(Vec3.atCenterOf(other));
		return BlockPos.containing(home == null ? real : home.logicalPose().transformPositionInverse(real));
	}

	/** A snapshot of how the sub-level under {@code pos} sits right now, safe to read from any thread. */
	public static Frame frame(@Nullable Level level, BlockPos pos) {
		SubLevelAccess sub = at(level, pos);
		return sub == null ? Frame.GROUND : Frame.of(sub.logicalPose());
	}

	/**
	 * Where a sub-level stood at one moment (or {@link #GROUND}: the plain world). Immutable, so the voice thread can
	 * place audio with it while the game thread moves on.
	 */
	public static final class Frame {

		public static final Frame GROUND = new Frame(null, null, null, null);

		@Nullable private final Vector3d position, rotationPoint, scale;
		@Nullable private final Quaterniond orientation;

		private Frame(@Nullable Vector3d position, @Nullable Quaterniond orientation, @Nullable Vector3d rotationPoint, @Nullable Vector3d scale) {
			this.position = position;
			this.orientation = orientation;
			this.rotationPoint = rotationPoint;
			this.scale = scale;
		}

		static Frame of(Pose3dc pose) {
			return new Frame(new Vector3d(pose.position()), new Quaterniond(pose.orientation()), new Vector3d(pose.rotationPoint()), new Vector3d(pose.scale()));
		}

		public boolean isGround() {
			return orientation == null;
		}

		/** Where {@code local} (a point in the sub-level's own block grid) really is. */
		public Vec3 apply(Vec3 local) {
			if(orientation == null || position == null || rotationPoint == null || scale == null)
				return local;
			Vector3d v = new Vector3d(local.x, local.y, local.z).sub(rotationPoint).mul(scale);
			orientation.transform(v).add(position);
			return new Vec3(v.x, v.y, v.z);
		}

		/** {@code local} as a direction (no position), turned with the sub-level. */
		public Vec3 applyDirection(Vec3 local) {
			if(orientation == null)
				return local;
			Vector3d v = orientation.transform(new Vector3d(local.x, local.y, local.z));
			return new Vec3(v.x, v.y, v.z);
		}
	}
}
