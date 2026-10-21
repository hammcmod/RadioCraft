package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.entities.IAntennaWire;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.SWRHelper;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.DipoleAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Off-centre-fed dipole (OCFD): a half-wave dipole fed about a third of the way along, on a 4:1 balun. The off-centre
 * feed point sees roughly 200 ohms, which the 4:1 brings down to the coax, and, unlike a centre-fed dipole, the same
 * wire is also matched on its even harmonics, so a 40m OCFD works on 40, 20 and 10m (not 15m). Built like a dipole:
 * two level arms in opposite directions, with the short arm between a third and two thirds of the long one.
 * <p>
 * Lengths are blocks (1 block = 1 m). The whole wire wants to be a half wave of the band in use, or 2 or 4 half
 * waves; every block off the nearest of those adds 0.5 to the SWR, which leaves the odd harmonics (3, 5...) a
 * poor match by themselves.
 */
public class OffCenterFedDipoleAntennaType extends NonDirectionalAntennaType<DipoleAntennaData> {

	/** Half-wave multiples the antenna is matched on (the fundamental and its even harmonics). */
	public static final int[] MATCHED_HARMONICS = { 1, 2, 4 };
	/** The short arm must be at least this fraction of the long one... */
	public static final double MIN_ARM_RATIO = 1.0D / 3.0D;
	/** ...and at most this: at equal arms it is a centre-fed dipole and belongs on a 1:1. */
	public static final double MAX_ARM_RATIO = 2.0D / 3.0D;

	public OffCenterFedDipoleAntennaType() {
		super(Radiocraft.id("off_center_fed_dipole"), 0.0D, 0.0D, 1.0D, 1.0D);
	}

	@Override
	public StaticAntenna<DipoleAntennaData> match(Level level, BlockPos pos) {
		if(level.getBlockState(pos).getBlock() != RadiocraftBlocks.BALUN_FOUR_TO_ONE.get())
			return null; // The 4:1 balun is what makes it an OCFD.
		List<IAntennaWire> connections = AntennaWire.getWires(level, pos);
		if(connections.size() != 2)
			return null;

		BlockPos arm1 = connections.get(0).getEndPos();
		BlockPos arm2 = connections.get(1).getEndPos();
		if(arm1.getY() != pos.getY() || arm2.getY() != pos.getY())
			return null; // Both arms level with the balun, like a dipole.
		if(AntennaWire.getWires(level, arm1).size() > 1 || AntennaWire.getWires(level, arm2).size() > 1)
			return null; // Neither arm continues.

		BlockPos relative1 = arm1.subtract(pos);
		BlockPos relative2 = arm2.subtract(pos);
		Vec3 direction1 = new Vec3(relative1.getX(), 0.0D, relative1.getZ()).normalize();
		Vec3 direction2 = new Vec3(relative2.getX(), 0.0D, relative2.getZ()).normalize();
		if(direction1.dot(direction2) > -0.833D)
			return null; // Arms opposite to within 15 degrees.

		double length1 = new Vec3(relative1.getX(), 0.0D, relative1.getZ()).length();
		double length2 = new Vec3(relative2.getX(), 0.0D, relative2.getZ()).length();
		if(!isOffCentre(length1, length2))
			return null; // Equal arms are a dipole on the wrong balun, a tiny arm is an end-fed.
		return new StaticAntenna<>(this, pos, level, new DipoleAntennaData(length1, length2));
	}

	/** True if the shorter arm is between a third and two thirds of the longer one. */
	public static boolean isOffCentre(double length1, double length2) {
		double shorter = Math.min(length1, length2);
		double longer = Math.max(length1, length2);
		if(longer <= 0.0D)
			return false;
		double ratio = shorter / longer;
		return ratio >= MIN_ARM_RATIO - 1e-9 && ratio <= MAX_ARM_RATIO + 1e-9;
	}

	@Override
	public double getSWR(DipoleAntennaData data, float frequencyHertz) {
		return swrForLength(data.getArmLength1() + data.getArmLength2(), frequencyHertz);
	}

	/**
	 * SWR of an OCFD of total length {@code length} blocks at {@code frequencyHertz}: 1 when the wire is a half wave
	 * or 2 or 4 of them (rounded to blocks), plus 0.5 per block off the nearest of those. An odd harmonic (3 half
	 * waves, say) is a whole half wave away from the nearest match, so it comes out poor on its own.
	 * {@link SWRHelper#OVERDRAW_SWR} for a wire shorter than a quarter wave, which is not an antenna for that band.
	 */
	public static double swrForLength(double length, float frequencyHertz) {
		double halfWave = BandUtils.getWavelengthMetersFromFrequencyHertz(frequencyHertz) / 2.0D;
		if(length < halfWave / 2.0D)
			return SWRHelper.OVERDRAW_SWR;
		double best = Double.MAX_VALUE;
		for(int harmonic : MATCHED_HARMONICS)
			best = Math.min(best, 1.0D + 0.5D * Math.abs(length - Math.round(harmonic * halfWave)));
		return best;
	}

	@Override
	public DipoleAntennaData getDefaultData() {
		return new DipoleAntennaData(0, 0);
	}
}
