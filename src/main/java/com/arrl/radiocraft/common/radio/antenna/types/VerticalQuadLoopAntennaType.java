package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.entities.IAntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.Antenna;
import com.arrl.radiocraft.common.radio.antenna.AntennaMorsePacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.radio.antenna.types.data.VerticalQuadLoopAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.List;

public class VerticalQuadLoopAntennaType implements IAntennaType<VerticalQuadLoopAntennaData> {

	public static final ResourceLocation ID = Radiocraft.location("vertical_quad_loop");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public Antenna<VerticalQuadLoopAntennaData> match(Level level, BlockPos pos) {
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
		return new Antenna<>(this, pos, new VerticalQuadLoopAntennaData(sideLength, xAxis));
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
	public double getSSBTransmitStrength(AntennaVoicePacket packet, VerticalQuadLoopAntennaData data, BlockPos destination) {
		double distance = Math.sqrt(packet.getSource().distSqr(destination));
		ServerLevel level = (ServerLevel)packet.getLevel().getServerLevel();

		double baseStrength = BandUtils.getSSBBaseStrength(packet.getWavelength(), distance, 1.25D, 1.25D, level.isDay());
		return baseStrength * getEfficiency(packet.getWavelength(), data, packet.getSource(), destination);
	}

	@Override
	public double getCWTransmitStrength(AntennaMorsePacket packet, VerticalQuadLoopAntennaData data, BlockPos destination) {
		double distance = Math.sqrt(packet.getSource().distSqr(destination));

		double baseStrength = BandUtils.getCWBaseStrength(packet.getWavelength(), distance, 1.0D, 1.0D, packet.getLevel().isDay());
		return baseStrength * getEfficiency(packet.getWavelength(), data, packet.getSource(), destination);
	}

	@Override
	public double getReceiveStrength(IAntennaPacket packet, VerticalQuadLoopAntennaData data, BlockPos pos) {
		return packet.getStrength() * getEfficiency(packet.getWavelength(), data, pos, packet.getSource());
	}

	public double getEfficiency(int wavelength, VerticalQuadLoopAntennaData data, BlockPos from, BlockPos to) {
		int desiredLength = (int)Math.round(wavelength / 4.0D); // The desired length for each "arm" is 1/4 of the wavelength used, round to the nearest int (for example 10m radio -> 3 blocks)
		double efficiency = desiredLength == data.getSideLength() ? 1.0D : 0.1D;

		BlockPos offset = to.subtract(from);
		Vec2 dir = new Vec2(offset.getX(), offset.getZ()).normalized();
		double f = 1.0D - Math.abs(data.getXAxis() ? Vec2.UNIT_X.dot(dir) : Vec2.UNIT_Y.dot(dir)); // Nears 1 as the offset becomes perpendicular

		if(f > 0.5D)
			efficiency *= f * 0.75D; // 75% worse peformance on broadsides, scaled linearly with a 0.5 (45 degree) threshold.
		else
			efficiency *= 1.25D; // 25% better performance when not on broadside.

		return efficiency;
	}

	@Override
	public VerticalQuadLoopAntennaData getDefaultData() {
		return new VerticalQuadLoopAntennaData(0, false);
	}
}
