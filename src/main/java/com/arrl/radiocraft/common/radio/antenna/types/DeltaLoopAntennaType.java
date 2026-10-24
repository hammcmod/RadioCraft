package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.entities.IAntennaWire;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.DeltaLoopAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Delta loop (doc/DeltaLoopAndMoxonPlan.md): a closed triangle of antenna wire, one wavelength round, fed at a corner
 * through a 4:1 balun. The balun's two wires go to two corner holders, joined by a third wire. Any orientation: flat,
 * upright (apex up or down) or sloping. It is the long-distance loop: weak up close (line of sight x0.1) and strong
 * on skip (x1.5).
 * <p>
 * SWR: 1, plus 0.5 per block the perimeter is off one wavelength, plus 0.25 per block the longest side outgrows the
 * shortest (a lopsided triangle matches worse). Lower than {@link #MIN_HEIGHT} blocks above the ground it still
 * works, as the maintainer asked, but badly: SWR 0.5 worse and much less skip (x0.4 three blocks up, down to x0.1 on
 * the ground).
 */
public class DeltaLoopAntennaType extends NonDirectionalAntennaType<DeltaLoopAntennaData> {

	/** Blocks of clear air under the lowest corner for the loop to work properly. */
	public static final int MIN_HEIGHT = 4;
	public static final double LOW_SWR_PENALTY = 0.5D;
	private static final double CW_RANGE_MULTIPLIER = 1.5D; // As in NonDirectionalAntennaType.

	public DeltaLoopAntennaType() {
		super(Radiocraft.id("delta_loop"), 1.0D, 1.0D, 0.1D, 1.5D);
	}

	@Override
	public StaticAntenna<DeltaLoopAntennaData> match(Level level, BlockPos pos) {
		if(level.getBlockState(pos).getBlock() != RadiocraftBlocks.BALUN_FOUR_TO_ONE.get())
			return null; // The 4:1 balun feeds the loop's corner.
		List<IAntennaWire> fromBalun = AntennaWire.getWires(level, pos);
		if(fromBalun.size() != 2)
			return null;
		BlockPos a = fromBalun.get(0).getEndPos();
		BlockPos b = fromBalun.get(1).getEndPos();
		if(a.equals(b) || !joined(level, a, b))
			return null; // Two open arms (an OCFD) or anything else: not a closed triangle.

		double side1 = Math.sqrt(pos.distSqr(a)), side2 = Math.sqrt(a.distSqr(b)), side3 = Math.sqrt(b.distSqr(pos));
		int height = Math.min(heightAboveGround(level, pos), Math.min(heightAboveGround(level, a), heightAboveGround(level, b)));
		return new StaticAntenna<>(this, pos, level, new DeltaLoopAntennaData(side1 + side2 + side3,
				Math.max(side1, Math.max(side2, side3)), Math.min(side1, Math.min(side2, side3)), height));
	}

	/** True if corner {@code a} has exactly two wires, one back to the balun and one to corner {@code b}, and so has {@code b}. */
	private static boolean joined(Level level, BlockPos a, BlockPos b) {
		List<IAntennaWire> atA = AntennaWire.getWires(level, a);
		List<IAntennaWire> atB = AntennaWire.getWires(level, b);
		return atA.size() == 2 && atB.size() == 2 && atA.stream().anyMatch(wire -> wire.getEndPos().equals(b));
	}

	/** Blocks of air (or wire holders: poles and connectors) under {@code pos} down to the first solid block. */
	public static int heightAboveGround(Level level, BlockPos pos) {
		BlockPos.MutableBlockPos below = pos.mutable();
		for(int height = 0; height < 64; height++) {
			below.move(0, -1, 0);
			if(below.getY() < level.getMinBuildHeight())
				return height;
			BlockState state = level.getBlockState(below);
			if(!state.isAir() && !state.is(RadiocraftTags.Blocks.ANTENNA_WIRE_HOLDERS))
				return height;
		}
		return 64;
	}

	/** True if the loop hangs too low to work properly. */
	public static boolean isTooLow(DeltaLoopAntennaData data) {
		return data.getHeight() < MIN_HEIGHT;
	}

	/** What is left of the loop's skip when it hangs {@code height} blocks up: all of it from 4, 0.4 at 3 down to 0.1 on the ground. */
	public static double lowSkipFactor(int height) {
		return height >= MIN_HEIGHT ? 1.0D : 0.1D + 0.1D * Math.max(0, height);
	}

	@Override
	public double getTransmitEfficiency(IAntennaPacket packet, DeltaLoopAntennaData data, BlockPos destination, boolean isCW) {
		double efficiency = super.getTransmitEfficiency(packet, data, destination, isCW);
		double distance = computeDistance(packet, destination) / (isCW ? CW_RANGE_MULTIPLIER : 1.0D);
		return isSkip(packet.getBand(), distance) ? efficiency * lowSkipFactor(data.getHeight()) : efficiency;
	}

	@Override
	public double getReceiveEfficiency(IAntennaPacket packet, DeltaLoopAntennaData data, BlockPos pos) {
		double efficiency = super.getReceiveEfficiency(packet, data, pos);
		return isSkip(packet.getBand(), computeDistance(packet, pos)) ? efficiency * lowSkipFactor(data.getHeight()) : efficiency;
	}

	private static boolean isSkip(Band band, double distance) {
		return band != null && distance >= band.losRange();
	}

	@Override
	public double getSWR(DeltaLoopAntennaData data, float frequencyHertz) {
		double wavelength = BandUtils.getWavelengthMetersFromFrequencyHertz(frequencyHertz);
		double swr = 1.0D + 0.5D * Math.abs(data.getPerimeter() - wavelength) + 0.25D * (data.getLongestSide() - data.getShortestSide());
		return isTooLow(data) ? swr + LOW_SWR_PENALTY : swr;
	}

	@Override
	public DeltaLoopAntennaData getDefaultData() {
		return new DeltaLoopAntennaData(0.0D, 0.0D, 0.0D, 0);
	}
}
