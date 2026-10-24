package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.entities.IAntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.MoxonAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Moxon (doc/DeltaLoopAndMoxonPlan.md): a two-element beam bent into a rectangle, built flat and level.
 * <pre>
 *   R---------R        back: the reflector, a separate wire run, its ends (r) folded forward
 *   r         r
 *   (1 block gap)
 *   d         d        the driven element's ends (d) folded back
 *   D----B----D        front: a 1:1 balun B feeding the driven element
 * </pre>
 * The balun's two wires run opposite ways to the front corners D; from each D one more wire runs back, at a right
 * angle and the same length on both sides, to a tail end d. One block further back each r starts the reflector: a
 * tail forward from its corner R (or none, if the reflector is just the back wire R-R), and a wire across between the
 * corners. Without the reflector it is a bent dipole, and the other types (or the improvised one) take it.
 * <p>
 * The beam points from the reflector to the driven element: 1.5 forward, falling to 0.15 at the sides and 0.15 behind,
 * on transmit and receive, line of sight and skip alike. SWR: 1, plus 0.5 per block the width and the depth are off
 * the band's size (20m 7 x 3, 10m 3 x 2; other bands about a third and a seventh of a wavelength).
 */
public class MoxonAntennaType extends NonDirectionalAntennaType<MoxonAntennaData> {

	public static final double FORWARD = 1.5D;
	public static final double BEHIND = 0.15D;

	public MoxonAntennaType() {
		super(Radiocraft.id("moxon"), 0.0D, 0.0D, 1.0D, 1.0D);
	}

	@Override
	public StaticAntenna<MoxonAntennaData> match(Level level, BlockPos pos) {
		if(level.getBlockState(pos).getBlock() != RadiocraftBlocks.BALUN_ONE_TO_ONE.get())
			return null;
		List<IAntennaWire> fromBalun = AntennaWire.getWires(level, pos);
		if(fromBalun.size() != 2)
			return null;
		BlockPos front1 = fromBalun.get(0).getEndPos(), front2 = fromBalun.get(1).getEndPos();
		if(front1.getY() != pos.getY() || front2.getY() != pos.getY())
			return null;
		Direction across = axis(front2.subtract(front1));
		if(across == null || axis(front1.subtract(pos)) != across.getOpposite() || axis(front2.subtract(pos)) != across)
			return null; // The driven element runs straight across, through the balun.

		BlockPos tail1 = driverTail(level, front1, pos), tail2 = driverTail(level, front2, pos);
		if(tail1 == null || tail2 == null)
			return null;
		Direction back = axis(tail1.subtract(front1));
		if(back == null || back.getAxis() == across.getAxis() || !tail2.subtract(front2).equals(tail1.subtract(front1)))
			return null; // Both tails fold back the same way, the same length, at a right angle.

		BlockPos reflector1 = reflectorCorner(level, tail1.relative(back), back);
		if(reflector1 == null)
			return null;
		BlockPos reflector2 = reflector1.offset(front2.subtract(front1));
		if(AntennaWire.getWires(level, reflector1).stream().noneMatch(wire -> wire.getEndPos().equals(reflector2)))
			return null; // No reflector across the back: a bent dipole, not a Moxon.
		BlockPos start2 = tail2.relative(back);
		if(!reflector2.equals(start2) && AntennaWire.getWires(level, reflector2).stream().noneMatch(wire -> wire.getEndPos().equals(start2)))
			return null; // The reflector's other tail is missing.

		int width = (int)Math.round(Math.sqrt(front1.distSqr(front2)));
		int depth = Math.abs(dot(reflector1.subtract(front1), back));
		return new StaticAntenna<>(this, pos, level, new MoxonAntennaData(width, depth, back.getOpposite()));
	}

	/** The far end of a front corner's folded tail: the corner has exactly its wire from the balun and the tail. */
	@Nullable
	private static BlockPos driverTail(Level level, BlockPos corner, BlockPos balun) {
		List<IAntennaWire> wires = AntennaWire.getWires(level, corner);
		if(wires.size() != 2)
			return null;
		BlockPos tail = wires.get(0).getEndPos().equals(balun) ? wires.get(1).getEndPos() : wires.get(0).getEndPos();
		if(tail.getY() != corner.getY() || AntennaWire.getWires(level, tail).size() != 1)
			return null; // The tail ends there.
		return tail;
	}

	/**
	 * The reflector corner behind the gap: {@code start} itself when the reflector has no tails, or the end of the
	 * tail running back from {@code start}.
	 */
	@Nullable
	private static BlockPos reflectorCorner(Level level, BlockPos start, Direction back) {
		List<IAntennaWire> wires = AntennaWire.getWires(level, start);
		if(wires.isEmpty())
			return null;
		for(IAntennaWire wire : wires)
			if(wire.getEndPos().getY() == start.getY() && axis(wire.getEndPos().subtract(start)) == back)
				return wires.size() == 1 ? wire.getEndPos() : null; // A tail: it only goes back to its corner.
		return start;
	}

	/** The direction a horizontal, axis-aligned offset points, or null if it is diagonal, vertical or zero. */
	@Nullable
	private static Direction axis(BlockPos offset) {
		if(offset.getY() != 0 || (offset.getX() != 0) == (offset.getZ() != 0))
			return null;
		return Direction.getNearest(offset.getX(), 0, offset.getZ());
	}

	private static int dot(BlockPos offset, Direction direction) {
		return offset.getX() * direction.getStepX() + offset.getZ() * direction.getStepZ();
	}

	/** The beam: 1.5 within 45 degrees of where it points, falling to 0.15 at the sides, 0.15 behind. */
	public static double pattern(Direction facing, BlockPos antenna, BlockPos other) {
		double dx = other.getX() - antenna.getX(), dz = other.getZ() - antenna.getZ();
		double length = Math.sqrt(dx * dx + dz * dz);
		if(length < 1e-6)
			return 1.0D;
		double cos = (dx * facing.getStepX() + dz * facing.getStepZ()) / length;
		double angle = Math.toDegrees(Math.acos(Math.max(-1.0D, Math.min(1.0D, cos))));
		if(angle <= 45.0D)
			return FORWARD;
		if(angle >= 90.0D)
			return BEHIND;
		return FORWARD + (BEHIND - FORWARD) * (angle - 45.0D) / 45.0D;
	}

	@Override
	public double getTransmitEfficiency(IAntennaPacket packet, MoxonAntennaData data, BlockPos destination, boolean isCW) {
		return super.getTransmitEfficiency(packet, data, destination, isCW) * pattern(data.getFacing(), packet.getSource().getAntennaPos().position(), destination);
	}

	@Override
	public double getReceiveEfficiency(IAntennaPacket packet, MoxonAntennaData data, BlockPos pos) {
		return super.getReceiveEfficiency(packet, data, pos) * pattern(data.getFacing(), pos, packet.getSource().getAntennaPos().position());
	}

	/** The width and depth a Moxon wants on a band. */
	public static int[] size(@Nullable Band band, float frequencyHertz) {
		if(band != null && band.name().equals("20m"))
			return new int[] {7, 3};
		if(band != null && band.name().equals("10m"))
			return new int[] {3, 2};
		double wavelength = BandUtils.getWavelengthMetersFromFrequencyHertz(frequencyHertz);
		return new int[] {(int)Math.round(wavelength / 3.0D), (int)Math.round(wavelength / 7.0D)};
	}

	@Override
	public double getSWR(MoxonAntennaData data, float frequencyHertz) {
		int[] wanted = size(Band.getBand(frequencyHertz), frequencyHertz);
		return 1.0D + 0.5D * Math.abs(data.getWidth() - wanted[0]) + 0.5D * Math.abs(data.getDepth() - wanted[1]);
	}

	@Override
	public MoxonAntennaData getDefaultData() {
		return new MoxonAntennaData(0, 0, Direction.NORTH);
	}
}
