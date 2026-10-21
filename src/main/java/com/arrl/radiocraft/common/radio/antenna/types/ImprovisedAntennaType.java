package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.entities.IAntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.ImprovisedAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The antenna for everything else: any wires on any balun that none of the proper antenna types recognise. In ham radio
 * any antenna beats no antenna, so this always works, just with a worse SWR the further the build is from an ideal
 * shape. It is tried after every other type ({@link com.arrl.radiocraft.api.antenna.AntennaTypes#registerFallbackType}).
 * <p>
 * SWR = 1, plus 0.5 per block each arm is off a resonant length (an odd multiple of a quarter wave, as a dipole arm),
 * plus the shape: arms that are not opposite (up to {@link #ANGLE_PENALTY} when folded back on each other), arms that
 * slope (up to {@link #SLOPE_PENALTY} straight up or down), each extra arm and each branch, and a balun of the wrong
 * ratio. A dipole whose arm dips a block comes out barely worse than a dipole; a tangle comes out near SWR 10 and wants
 * a tuner. Capped at {@link #MAX_SWR}.
 */
public class ImprovisedAntennaType extends NonDirectionalAntennaType<ImprovisedAntennaData> {

	/** Half the signal of a dipole, like the random wire: nothing about it is optimised. */
	public static final double GAIN_DBI = -3.0D;
	public static final double ANGLE_PENALTY = 3.0D;
	public static final double SLOPE_PENALTY = 1.0D;
	public static final double EXTRA_ARM_PENALTY = 1.0D;
	public static final double BRANCH_PENALTY = 0.5D;
	public static final double MAX_SWR = 30.0D;

	public ImprovisedAntennaType() {
		super(Radiocraft.id("improvised"), GAIN_DBI, GAIN_DBI, 1.0D, 1.0D);
	}

	@Override
	public StaticAntenna<ImprovisedAntennaData> match(Level level, BlockPos pos) {
		int ratio = balunRatio(level.getBlockState(pos).getBlock());
		if(ratio == 0)
			return null;
		List<IAntennaWire> connections = AntennaWire.getWires(level, pos);
		if(connections.isEmpty())
			return null;

		List<Arm> arms = new ArrayList<>();
		Set<BlockPos> leaving = new HashSet<>();
		for(IAntennaWire wire : connections) {
			BlockPos next = InvertedVAntennaType.otherEnd(wire, pos);
			if(next != null && leaving.add(next)) // A wire and its end part both report here; count each run once.
				arms.add(walk(level, pos, wire));
		}
		if(arms.isEmpty())
			return null;
		arms.sort((a, b) -> Double.compare(b.length, a.length));

		double penalty = 0.0D;
		for(Arm arm : arms)
			penalty += arm.branches * BRANCH_PENALTY;
		if(arms.size() >= 2) {
			Arm a = arms.get(0), b = arms.get(1);
			penalty += anglePenalty(a.direction, b.direction) + slopePenalty(a.direction) + slopePenalty(b.direction);
			penalty += (arms.size() - 2) * EXTRA_ARM_PENALTY;
		}

		double[] lengths = arms.stream().limit(2).mapToDouble(arm -> arm.length).toArray();
		return new StaticAntenna<>(this, pos, level, new ImprovisedAntennaData(lengths, penalty, ratio));
	}

	/** One run of wire out from the feed point: its length along the wire, the straight line to its far end, its branches. */
	private record Arm(double length, Vec3 direction, int branches) {}

	/** Follows a run from the feed point to its end, taking the first way on at a branch and stopping at a loop. */
	private static Arm walk(Level level, BlockPos feed, IAntennaWire first) {
		Set<BlockPos> visited = new HashSet<>();
		visited.add(feed);
		BlockPos at = feed;
		IAntennaWire wire = first;
		double length = 0.0D;
		int branches = 0;
		while(wire != null) {
			BlockPos next = InvertedVAntennaType.otherEnd(wire, at);
			if(next == null || !visited.add(next))
				break;
			length += wire.getLength();
			IAntennaWire continuation = null;
			Set<BlockPos> ways = new HashSet<>();
			for(IAntennaWire candidate : AntennaWire.getWires(level, next)) {
				BlockPos far = InvertedVAntennaType.otherEnd(candidate, next);
				if(far == null || far.equals(at) || !ways.add(far))
					continue;
				if(continuation == null)
					continuation = candidate;
			}
			branches += Math.max(0, ways.size() - 1);
			at = next;
			wire = continuation;
		}
		BlockPos end = at.subtract(feed);
		return new Arm(length, new Vec3(end.getX(), end.getY(), end.getZ()), branches);
	}

	/** 0 for arms pointing opposite ways in plan, up to {@link #ANGLE_PENALTY} for arms folded back on each other. */
	static double anglePenalty(Vec3 a, Vec3 b) {
		Vec3 flatA = new Vec3(a.x, 0.0D, a.z);
		Vec3 flatB = new Vec3(b.x, 0.0D, b.z);
		if(flatA.lengthSqr() < 1e-6 || flatB.lengthSqr() < 1e-6)
			return ANGLE_PENALTY / 2.0D; // An arm straight up or down: as good as perpendicular.
		double dot = flatA.normalize().dot(flatB.normalize());
		return (1.0D + dot) / 2.0D * ANGLE_PENALTY;
	}

	/** 0 for a level arm, up to {@link #SLOPE_PENALTY} for one straight up or down. */
	static double slopePenalty(Vec3 arm) {
		double length = arm.length();
		return length < 1e-6 ? 0.0D : Math.abs(arm.y) / length * SLOPE_PENALTY;
	}

	/** The balun ratio of a feed block, 0 if it is not a balun. */
	static int balunRatio(Block block) {
		if(block == RadiocraftBlocks.BALUN_ONE_TO_ONE.get())
			return 1;
		if(block == RadiocraftBlocks.BALUN_TWO_TO_ONE.get())
			return 2;
		if(block == RadiocraftBlocks.BALUN_FOUR_TO_ONE.get())
			return 4;
		if(block == RadiocraftBlocks.BALUN_NINE_TO_ONE.get())
			return 9;
		return 0;
	}

	@Override
	public double getSWR(ImprovisedAntennaData data, float frequencyHertz) {
		return swr(data.getArmLengths(), data.getShapePenalty(), data.getBalunRatio(), BandUtils.getWavelengthMetersFromFrequencyHertz(frequencyHertz));
	}

	/** See the class comment. Pure, for tests. */
	public static double swr(double[] armLengths, double shapePenalty, int balunRatio, double wavelength) {
		double swr = 1.0D + shapePenalty;
		double quarter = wavelength / 4.0D;
		for(double length : armLengths) {
			// Nearest odd multiple of a quarter wave (1/4, 3/4, ...): where a wire fed at one end is resonant.
			double odd = Math.max(1.0D, 2.0D * Math.round((length / quarter - 1.0D) / 2.0D) + 1.0D);
			swr += 0.5D * Math.abs(length - odd * quarter);
		}
		boolean endFed = armLengths.length == 1;
		swr += balunPenalty(balunRatio, endFed);
		return Math.min(MAX_SWR, swr);
	}

	/** Extra SWR for a balun of the wrong ratio: 1:1 suits two arms, a high ratio (9:1) suits a single wire. */
	static double balunPenalty(int ratio, boolean endFed) {
		if(endFed)
			return ratio >= 9 ? 0.0D : ratio >= 4 ? 0.5D : 1.0D;
		return switch(ratio) {
			case 1 -> 0.0D;
			case 2 -> 0.5D;
			case 4 -> 1.5D;
			default -> 4.0D;
		};
	}

	@Override
	public ImprovisedAntennaData getDefaultData() {
		return new ImprovisedAntennaData(new double[0], 0.0D, 1);
	}
}
