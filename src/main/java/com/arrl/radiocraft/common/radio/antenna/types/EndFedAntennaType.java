package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.entities.IAntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.EndFedAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class EndFedAntennaType extends  NonDirectionalAntennaType<EndFedAntennaData> {

	public EndFedAntennaType() {
		super(Radiocraft.id("end_fed"), -1.25D, -1.25D, 1.0D, 1.0D);
	}

	@Override
	public StaticAntenna<EndFedAntennaData> match(Level level, BlockPos pos) {
		if(level.getBlockState(pos).getBlock() != RadiocraftBlocks.BALUN_ONE_TO_ONE.get())
			return null; // Do not match if center block is not a 1:1 balun.

		List<IAntennaWire> connections = AntennaWire.getWires(level, pos);

		if(connections.size() != 1)
			return null; // Do not match if there is not only 1 connection to the center block.

		BlockPos end = connections.get(0).getEndPos();

		if(end.getY() != pos.getY())
			return null; // Do not match if arm goes up or down.

		if(AntennaWire.getWires(level, end).size() > 1)
			return null; // Do not match if arm continues.

		BlockPos relativeBlockPos = end.subtract(pos);
		Vec3 relative = new Vec3(relativeBlockPos.getX(), relativeBlockPos.getY(), relativeBlockPos.getZ());

		return new StaticAntenna<>(this, pos, level, new EndFedAntennaData(relative.length()));
	}

	@Override
	public double getSWR(EndFedAntennaData data, float frequencyHertz) {
		int desiredLength = (int)Math.round(BandUtils.getWavelengthMetersFromFrequencyHertz(frequencyHertz) / 4.0D); // The desired length for each "arm" is 1/4 of the wavelength used, round to the nearest int (for example 10m radio -> 3 blocks)
		double incorrectBlocks = Math.abs(desiredLength - data.getLength());

		return 1.0D + (0.5D * incorrectBlocks);
	}

	@Override
	public EndFedAntennaData getDefaultData() {
		return new EndFedAntennaData(0);
	}

}
