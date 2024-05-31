package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.entities.IAntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.types.data.DipoleAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class DipoleAntennaType extends NonDirectionalAntennaType<DipoleAntennaData> {

	public DipoleAntennaType() {
		super(Radiocraft.id("dipole"), 1.0D, 1.0D, 1.0D, 1.0D);
	}

	@Override
	public StaticAntenna<DipoleAntennaData> match(Level level, BlockPos pos) {
		if(level.getBlockState(pos).getBlock() != RadiocraftBlocks.BALUN_ONE_TO_ONE.get())
			return null; // Do not match if center block is not a 1:1 balun.

		List<IAntennaWire> connections = AntennaWire.getWires(level, pos);

		if(connections.size() != 2)
			return null; // Do not match if there are not 2 connections to the center block.

		BlockPos arm1 = connections.get(0).getEndPos();
		BlockPos arm2 = connections.get(1).getEndPos();

		if(arm1.getY() != pos.getY() || arm2.getY() != pos.getY())
			return null; // Do not match if either arm goes up or down.

		if(AntennaWire.getWires(level, arm1).size() > 1 || AntennaWire.getWires(level, arm2).size() > 1)
			return null; // Do not match if either arm continues.

		BlockPos relativeArm1BlockPos = arm1.subtract(pos);
		BlockPos relativeArm2BlockPos = arm2.subtract(pos);

		Vec3 relativeArm1 = new Vec3(relativeArm1BlockPos.getX(), relativeArm1BlockPos.getY(), relativeArm1BlockPos.getZ());
		Vec3 relativeArm2 = new Vec3(relativeArm2BlockPos.getX(), relativeArm2BlockPos.getY(), relativeArm2BlockPos.getZ());

		Vec3 directionArm1 = new Vec3(relativeArm1.x, 0.0D, relativeArm1.z).normalize();
		Vec3 directionArm2 = new Vec3(relativeArm2.x, 0.0D, relativeArm2.z).normalize();
		double dot = directionArm1.dot(directionArm2); // 1 = same dir, 0 = perpendicular, -1 = opposite.

		if(dot > -0.833D) // If dot is larger than this value then arms are not "opposite enough" (opposite to within +/- 15 degrees)
			return null;


		return new StaticAntenna<>(this, pos, new DipoleAntennaData(relativeArm1.length(), relativeArm2.length()));
	}

	@Override
	public double getSWR(DipoleAntennaData data, int wavelength) {
		int desiredLength = (int)Math.round(wavelength / 4.0D); // The desired length for each "arm" is 1/4 of the wavelength used, round to the nearest int (for example 10m radio -> 3 blocks)
		double incorrectBlocks = Math.abs(desiredLength - data.getArmLength1()) + Math.abs(desiredLength - data.getArmLength2());

		return 1.0D + (0.5D * incorrectBlocks);
	}

	@Override
	public DipoleAntennaData getDefaultData() {
		return new DipoleAntennaData(0, 0);
	}

}
