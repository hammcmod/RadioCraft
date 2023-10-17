package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.entities.IAntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.antenna.BEAntenna;
import com.arrl.radiocraft.common.radio.antenna.types.data.VerticalQuadLoopAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.List;

public class VerticalQuadLoopAntennaType extends DirectionalAntennaType<VerticalQuadLoopAntennaData> {

	public VerticalQuadLoopAntennaType() {
		super(Radiocraft.location("vertical_quad_loop"), 1.0D, 1.0D, 1.25D, 1.25D);
	}
	@Override
	public BEAntenna<VerticalQuadLoopAntennaData> match(Level level, BlockPos pos) {
		if(level.getBlockState(pos).getBlock() != RadiocraftBlocks.BALUN_TWO_TO_ONE.get())
			return null; // Do not match if center block is not a 2:1 balun.

		List<IAntennaWire> startConnections = AntennaWire.getWires(level, pos);

		if(startConnections.size() != 2)
			return null; // Do not match if balun doesn't have 2 connections.

		List<BlockPos> squarePoints = new ArrayList<>();
		List<IAntennaWire> squareWires = new ArrayList<>();
		IAntennaWire currentWire = startConnections.get(0);
		squarePoints.add(pos);
		squareWires.add(currentWire);

		BlockPos firstCorner = currentWire.getEndPos();
		boolean xAxis;
		if(firstCorner.getX() == pos.getX())
			xAxis = true;
		else if(firstCorner.getZ() == pos.getZ())
			xAxis = false;
		else
			return null; // Do not match if the first corner is not axis aligned

		for(int i = 0; i < 3; i++) { // Get the "corners" of the quad.
			BlockPos end = currentWire.getEndPos();

			if(xAxis) {
				if(end.getX() != pos.getX())
					return null;
			}
			else {
				if(end.getZ() != pos.getZ())
					return null; // Do not match if the corner does not have an equal value on whichever axis is being checked.
			}

			if(end.getY() < pos.getY())
				return null; // Do not match if not fed from bottom side of square.

			List<IAntennaWire> connections = AntennaWire.getWires(level, end);

			if(connections.size() != 2)
				return null; // Do not match if corner doesn't have 2 connections.

			currentWire = connections.get(0).isPairedWith(currentWire) ? connections.get(1) : connections.get(0);
			squarePoints.add(end);
			squareWires.add(currentWire);
		}

		if(!squareWires.get(3).getEndPos().equals(squarePoints.get(0)))
			return null; // Return null if the quad does not form a closed loop.

		if(!isSquare(squarePoints))
			return null; // Return null if the quad is not a square.

		int sideLength = (int)Math.sqrt(Math.round(squarePoints.get(0).distSqr(squarePoints.get(1))));
		return new BEAntenna<>(this, pos, new VerticalQuadLoopAntennaData(sideLength, xAxis));
	}

	public boolean isSquare(List<BlockPos> points) {
		if(points.size() != 4) // Cannot be a square if not a quadrilateral.
			return false;

		BlockPos p1 = points.get(0);
		BlockPos p2 = points.get(1);
		BlockPos p4 = points.get(3);

		double d2 = p1.distSqr(p2);

		return d2 == p1.distSqr(p4) && // If the 2 adjacent sides have equal length, and both diagonals obey pythagoras, the quad must be a square.
				2 * d2 == p1.distSqr(points.get(2)) &&
				2 * d2 == p2.distSqr(p4);
	}

	@Override
	public double getSWR(VerticalQuadLoopAntennaData data, int wavelength) {
		int desiredLength = (int)Math.round(wavelength / 4.0D);
		int incorrectBlocks = Math.abs(desiredLength - data.getSideLength());
		return 1.0D + (0.5D * incorrectBlocks);
	}

	@Override
	public double getDirectionalEfficiency(VerticalQuadLoopAntennaData data, BlockPos from, BlockPos to) {
		BlockPos offset = to.subtract(from);
		Vec2 dir = new Vec2(offset.getX(), offset.getZ()).normalized();
		double f = 1.0D - Math.abs(data.getXAxis() ? Vec2.UNIT_X.dot(dir) : Vec2.UNIT_Y.dot(dir)); // Nears 1 as the offset becomes perpendicular

		if(f > 0.5D)
			return f * 0.75D; // 75% worse peformance on broadsides, scaled linearly with a 0.5 (45 degree) threshold.
		else
			return f * 1.25D; // 25% better performance when not on broadside.
	}

	@Override
	public VerticalQuadLoopAntennaData getDefaultData() {
		return new VerticalQuadLoopAntennaData(0, false);
	}

}
