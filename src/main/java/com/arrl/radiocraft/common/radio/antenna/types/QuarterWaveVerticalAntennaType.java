package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.QuarterWaveVerticalAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;

public class QuarterWaveVerticalAntennaType extends NonDirectionalAntennaType<QuarterWaveVerticalAntennaData> {

	public QuarterWaveVerticalAntennaType() {
		super(Radiocraft.id("quarter_wave_vertical"), 0.0D, 0.0D, 1.2D, 0.7D);
	}

	@Override
	public StaticAntenna<QuarterWaveVerticalAntennaData> match(Level level, BlockPos pos) {
		if(level.getBlockState(pos).getBlock() != RadiocraftBlocks.BALUN_ONE_TO_ONE.get())
			return null; // Do not match if center block is not a 1:1 balun.

		if(!AntennaWire.getWires(level, pos).isEmpty())
			return null; // Do not match if there are any wires connected to center block.

		int height = getHeight(level, pos);
		if(height == 0)
			return null; // Do not match if there is no pole.

		return new StaticAntenna<>(this, pos, level, new QuarterWaveVerticalAntennaData(height));
	}

	public int getHeight(Level level, BlockPos pos) {
		MutableBlockPos _pos = pos.mutable();

		int poles = 0;
		boolean stop = false;
		while(!stop) {
			_pos.setY(_pos.getY() + 1);
			if(level.getBlockState(_pos).getBlock() == RadiocraftBlocks.ANTENNA_POLE.get())
				poles++;
			else
				stop = true;
		}
		return poles;
	}

	@Override
	public double getSWR(QuarterWaveVerticalAntennaData data, float frequencyHertz) {
		int desiredLength = (int)Math.round(BandUtils.getWavelengthMetersFromFrequencyHertz(frequencyHertz) / 4.0D);
		int incorrectBlocks = Math.abs(desiredLength - data.getHeight());

		return incorrectBlocks == 0 ? 1.0D : 10.0D;
	}

	@Override
	public QuarterWaveVerticalAntennaData getDefaultData() {
		return new QuarterWaveVerticalAntennaData(0);
	}

}
