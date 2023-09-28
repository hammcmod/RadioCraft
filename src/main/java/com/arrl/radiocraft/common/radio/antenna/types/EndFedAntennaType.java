package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.entities.IAntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.BEAntenna;
import com.arrl.radiocraft.common.radio.antenna.AntennaMorsePacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.radio.antenna.types.data.EndFedAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class EndFedAntennaType implements IAntennaType<EndFedAntennaData> {

	public static final ResourceLocation ID = Radiocraft.location("end_fed");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public BEAntenna<EndFedAntennaData> match(Level level, BlockPos pos) {
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

		return new BEAntenna<>(this, pos, new EndFedAntennaData(relative.length()));
	}

	@Override
	public double getSSBTransmitStrength(AntennaVoicePacket packet, EndFedAntennaData data, BlockPos destination) {
		double distance = Math.sqrt(packet.getSource().getPos().distSqr(destination));
		ServerLevel level = (ServerLevel)packet.getLevel().getServerLevel();

		double baseStrength = BandUtils.getSSBBaseStrength(packet.getWavelength(), distance, 1.0D, 1.0D, level.isDay());
		return baseStrength * getEfficiency(packet.getWavelength(), data) * 0.75D;
	}

	@Override
	public double getCWTransmitStrength(AntennaMorsePacket packet, EndFedAntennaData data, BlockPos destination) {
		double distance = Math.sqrt(packet.getSource().getPos().distSqr(destination));

		double baseStrength = BandUtils.getCWBaseStrength(packet.getWavelength(), distance, 1.0D, 1.0D, packet.getLevel().isDay());
		return baseStrength * getEfficiency(packet.getWavelength(), data);
	}

	@Override
	public double getReceiveStrength(IAntennaPacket packet, EndFedAntennaData data, BlockPos pos) {
		return packet.getStrength() * getEfficiency(packet.getWavelength(), data) * 0.75D;
	}

	public double getEfficiency(int wavelength, EndFedAntennaData data) {
		int desiredLength = (int)Math.round(wavelength / 2.0D); // The desired length is 1/2 of the wavelength used, round to nearest int.
		int incorrectBlocks = (int)Math.abs(desiredLength - data.getLength());

		return incorrectBlocks == 0 ? 1.0D : Math.pow(0.75D, incorrectBlocks);
	}

	@Override
	public EndFedAntennaData getDefaultData() {
		return new EndFedAntennaData(0);
	}

}
