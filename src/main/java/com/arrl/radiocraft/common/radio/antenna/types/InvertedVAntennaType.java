package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.entities.IAntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.InvertedVAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A dipole whose two arms slope down from the centre, so it only needs one tall support. The arms must be opposite
 * each other (within 15 degrees in plan), both ends must sit below the 1:1 balun, and each arm must run at least as
 * far sideways as it drops (an apex angle of 90 degrees or more). The ends may sit at different heights.
 *
 * <p>Resonant arm length is 5% shorter than a flat dipole's ({@link #LENGTH_FACTOR}), measured along the wire, and
 * the SWR rises 0.5 per block of error like the dipole. The sloping arms cost some signal compared to a flat dipole:
 * {@link #transmitPenalty} is 0.9 on most bands and 0.75 on 40m, the numbers from the original proposal.</p>
 */
public class InvertedVAntennaType extends NonDirectionalAntennaType<InvertedVAntennaData> {

	/** Resonant length relative to a flat dipole: the sloping arms couple to each other and want to be a little shorter. */
	public static final double LENGTH_FACTOR = 0.95D;
	private static final double OPPOSITE_DOT = -0.833D; // Arms opposite to within +/- 15 degrees in plan, as for the dipole.

	public InvertedVAntennaType() {
		super(Radiocraft.id("inverted_v"), 0.0D, 0.0D, 1.0D, 1.0D);
	}

	@Override
	public StaticAntenna<InvertedVAntennaData> match(Level level, BlockPos pos) {
		if(level.getBlockState(pos).getBlock() != RadiocraftBlocks.BALUN_ONE_TO_ONE.get())
			return null; // Centre must be a 1:1 balun.

		List<IAntennaWire> connections = AntennaWire.getWires(level, pos);
		if(connections.size() != 2)
			return null; // Exactly two arms.

		BlockPos end1 = otherEnd(connections.get(0), pos);
		BlockPos end2 = otherEnd(connections.get(1), pos);
		if(end1 == null || end2 == null)
			return null;

		int drop1 = pos.getY() - end1.getY();
		int drop2 = pos.getY() - end2.getY();
		if(drop1 < 1 || drop2 < 1)
			return null; // Both ends below the centre, otherwise it is a flat dipole (or nothing).

		if(AntennaWire.getWires(level, end1).size() > 1 || AntennaWire.getWires(level, end2).size() > 1)
			return null; // Arms must not continue.

		Vec3 arm1 = Vec3.atLowerCornerOf(end1.subtract(pos));
		Vec3 arm2 = Vec3.atLowerCornerOf(end2.subtract(pos));
		Vec3 flat1 = new Vec3(arm1.x, 0.0D, arm1.z);
		Vec3 flat2 = new Vec3(arm2.x, 0.0D, arm2.z);
		if(flat1.length() < drop1 || flat2.length() < drop2)
			return null; // Steeper than 45 degrees is a sloper, not an inverted V.

		if(flat1.normalize().dot(flat2.normalize()) > OPPOSITE_DOT)
			return null; // Arms not opposite enough in plan.

		return new StaticAntenna<>(this, pos, level, new InvertedVAntennaData(arm1.length(), arm2.length(), drop1, drop2));
	}

	/** The end of a wire that is not at {@code pos}, or null if the wire does not touch {@code pos}. */
	static BlockPos otherEnd(IAntennaWire wire, BlockPos pos) {
		if(wire.getStartPos().equals(pos))
			return wire.getEndPos();
		if(wire.getEndPos().equals(pos))
			return wire.getStartPos();
		return null;
	}

	/** Resonant arm length in blocks for a frequency: a quarter wave shortened by {@link #LENGTH_FACTOR}, to whole blocks. */
	public static int desiredArmLength(float frequencyHertz) {
		return (int)Math.round(BandUtils.getWavelengthMetersFromFrequencyHertz(frequencyHertz) / 4.0D * LENGTH_FACTOR);
	}

	@Override
	public double getSWR(InvertedVAntennaData data, float frequencyHertz) {
		int desiredLength = desiredArmLength(frequencyHertz);
		double incorrectBlocks = Math.abs(desiredLength - data.getArmLength1()) + Math.abs(desiredLength - data.getArmLength2());
		return 1.0D + (0.5D * incorrectBlocks);
	}

	/** Signal relative to a flat dipole on the same band: 0.75 on 40m, 0.9 elsewhere. */
	public static double transmitPenalty(Band band) {
		if(band == null)
			return 1.0D;
		return band.name().equals("40m") ? 0.75D : 0.9D;
	}

	@Override
	public double getTransmitEfficiency(IAntennaPacket packet, InvertedVAntennaData data, BlockPos destination, boolean isCW) {
		return super.getTransmitEfficiency(packet, data, destination, isCW) * transmitPenalty(packet.getBand());
	}

	@Override
	public InvertedVAntennaData getDefaultData() {
		return new InvertedVAntennaData(0, 0, 0, 0);
	}

}
