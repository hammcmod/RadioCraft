package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.antenna.Antenna;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetworkPacket;
import com.arrl.radiocraft.common.radio.antenna.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.types.data.QuarterWaveVerticalAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class QuarterWaveVerticalAntennaType implements IAntennaType<QuarterWaveVerticalAntennaData> {

	public static final ResourceLocation ID = Radiocraft.location("quarter_wave_vertical");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public Antenna<QuarterWaveVerticalAntennaData> match(Level level, BlockPos pos) {
		if(level.getBlockState(pos).getBlock() != RadiocraftBlocks.BALUN_ONE_TO_ONE.get())
			return null; // Do not match if center block is not a 1:1 balun.

		if(AntennaWire.getWires(level, pos).size() > 0)
			return null; // Do not match if there are any wires connected to center block.

		int height = getHeight(level, pos);
		if(height == 0)
			return null; // Do not match if there is no pole.

		return new Antenna<>(this, pos, new QuarterWaveVerticalAntennaData(height));
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
	public double getTransmitStrength(AntennaNetworkPacket packet, QuarterWaveVerticalAntennaData data, BlockPos destination) {
		double distance = Math.sqrt(packet.getSource().distSqr(destination));
		ServerLevel level = (ServerLevel)packet.getLevel().getServerLevel();

		double baseStrength = BandUtils.getBaseStrength(packet.getWavelength(), distance, 1.2D, 0.7D, level.isDay());
		return baseStrength * getEfficiency(packet, data);
	}

	@Override
	public double getReceiveStrength(AntennaNetworkPacket packet, QuarterWaveVerticalAntennaData data, BlockPos pos) {
		return packet.getStrength() * getEfficiency(packet, data);
	}

	public double getEfficiency(AntennaNetworkPacket packet, QuarterWaveVerticalAntennaData data) {
		int desiredLength = (int)Math.round(packet.getWavelength() / 4.0D); // The desired length for the pole is 1/4 of the wavelength used, round to the nearest int (for example 10m radio -> 3 blocks)
		int incorrectBlocks = Math.abs(desiredLength - data.getHeight());

		return incorrectBlocks == 0 ? 1.0D : Math.pow(0.75D, incorrectBlocks);
	}

	@Override
	public QuarterWaveVerticalAntennaData getDefaultData() {
		return new QuarterWaveVerticalAntennaData(0);
	}

}
