package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.entities.IAntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.SWRHelper;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.RandomWireAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A random wire: a single run of antenna wire of any length and shape fed through a 9:1 balun. The run may bend at
 * poles and connectors but must not branch or loop back. It is the "whatever you can string up" antenna: it works on
 * every band the wire is at least half a wavelength long for, but it is never resonant on its own
 * ({@link #BASE_SWR}) and so needs an antenna tuner, and it loses half its signal ({@link #GAIN_DBI}) against a
 * dipole.
 *
 * <p>SWR is {@link #BASE_SWR} plus 0.5 per block the wire is away from a multiple of a half wavelength, or
 * {@link SWRHelper#OVERDRAW_SWR} when the wire is shorter than half a wavelength.</p>
 */
public class RandomWireAntennaType extends NonDirectionalAntennaType<RandomWireAntennaData> {

	/** Best case SWR: a random wire is never a match on its own, so a tuner is always needed. */
	public static final double BASE_SWR = 2.0D;
	/** Half the signal of a dipole, the proposal's "at least 50% of the range". */
	public static final double GAIN_DBI = -3.0D;

	public RandomWireAntennaType() {
		super(Radiocraft.id("random_wire"), GAIN_DBI, GAIN_DBI, 1.0D, 1.0D);
	}

	@Override
	public StaticAntenna<RandomWireAntennaData> match(Level level, BlockPos pos) {
		if(level.getBlockState(pos).getBlock() != RadiocraftBlocks.BALUN_NINE_TO_ONE.get())
			return null; // Fed through a 9:1 balun; the 1:1 belongs to the resonant antennas.

		List<IAntennaWire> connections = AntennaWire.getWires(level, pos);
		if(connections.size() != 1)
			return null; // One wire leaves the balun.

		Set<BlockPos> visited = new HashSet<>();
		visited.add(pos);
		BlockPos at = pos;
		IAntennaWire wire = connections.getFirst();
		double length = 0.0D;
		int segments = 0;

		while(wire != null) {
			BlockPos next = InvertedVAntennaType.otherEnd(wire, at);
			if(next == null || !visited.add(next))
				return null; // Broken wire or the run loops back on itself.
			length += wire.getLength();
			segments++;

			IAntennaWire continuation = null;
			for(IAntennaWire candidate : AntennaWire.getWires(level, next)) {
				if(isSameRun(candidate, at, next))
					continue; // The wire we arrived on (an AntennaWire and its end part both report here).
				if(continuation != null && !isSameRun(candidate, next, InvertedVAntennaType.otherEnd(continuation, next)))
					return null; // Branches: more than one wire leaves this point.
				continuation = candidate;
			}
			at = next;
			wire = continuation;
		}

		return new StaticAntenna<>(this, pos, level, new RandomWireAntennaData(length, segments));
	}

	/** True if the wire runs between a and b (in either direction). */
	private static boolean isSameRun(IAntennaWire wire, BlockPos a, BlockPos b) {
		if(b == null)
			return false;
		return (wire.getStartPos().equals(a) && wire.getEndPos().equals(b)) || (wire.getStartPos().equals(b) && wire.getEndPos().equals(a));
	}

	@Override
	public double getSWR(RandomWireAntennaData data, float frequencyHertz) {
		return swrForLength(data.getLength(), frequencyHertz);
	}

	/** See the class comment: unusable below half a wavelength, otherwise {@link #BASE_SWR} plus 0.5 per block off a half wave multiple. */
	public static double swrForLength(double length, float frequencyHertz) {
		double halfWave = BandUtils.getWavelengthMetersFromFrequencyHertz(frequencyHertz) / 2.0D;
		if(length < halfWave)
			return SWRHelper.OVERDRAW_SWR;
		double multiples = Math.max(1, Math.round(length / halfWave));
		double off = Math.abs(length - multiples * halfWave);
		return BASE_SWR + 0.5D * off;
	}

	@Override
	public RandomWireAntennaData getDefaultData() {
		return new RandomWireAntennaData(0, 0);
	}

}
