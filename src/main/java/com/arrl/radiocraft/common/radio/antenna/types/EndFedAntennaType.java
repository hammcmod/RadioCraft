package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.antenna.Antenna;
import com.arrl.radiocraft.common.radio.antenna.AntennaData;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetworkPacket;
import com.arrl.radiocraft.common.radio.antenna.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.types.EndFedAntennaType.EndFedAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class EndFedAntennaType implements IAntennaType<EndFedAntennaData> {

	public static final ResourceLocation ID = Radiocraft.location("end_fed");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public Antenna<EndFedAntennaData> match(Level level, BlockPos pos) {
		if(level.getBlockState(pos).getBlock() != RadiocraftBlocks.BALUN_ONE_TO_ONE.get())
			return null; // Do not match if center block is not a 1:1 balun.

		List<BlockPos> endPoints = new ArrayList<>();

		AntennaWire.getAntennaWires(level, pos).forEach(wire -> endPoints.add(wire.getEndPos())); // Grab ends and starts separately to avoid needing to check if each pos is the start or end of the entity.
		AntennaWire.getAntennaWireParts(level, pos).forEach(part -> endPoints.add(part.parent.blockPosition()));

		if(endPoints.size() != 1)
			return null; // Do not match if there is not only 1 connection to the center block.

		BlockPos end = endPoints.get(0);;

		if(end.getY() != pos.getY())
			return null; // Do not match if arm goes up or down.

		if(AntennaWire.getWires(level, end).size() > 1)
			return null; // Do not match if arm continues.

		BlockPos relativeBlockPos = end.subtract(pos);
		Vec3 relative = new Vec3(relativeBlockPos.getX(), relativeBlockPos.getY(), relativeBlockPos.getZ());

		return new Antenna<>(this, pos, new EndFedAntennaData(relative.length()));
	}

	@Override
	public void applyTransmitStrength(AntennaNetworkPacket packet, EndFedAntennaData data, BlockPos destination) {
		double efficiency = getEfficiency(packet, data);
		double distance = Math.sqrt(packet.getSource().distSqr(destination));
		ServerLevel level = (ServerLevel)packet.getLevel().getServerLevel();

		double baseStrength = BandUtils.getBaseStrength(packet.getWavelength(),distance, level.isDay());
		packet.setStrength(baseStrength * efficiency * 0.75D);
	}

	@Override
	public void applyReceiveStrength(AntennaNetworkPacket packet, EndFedAntennaData data, BlockPos pos) {
		double efficiency = getEfficiency(packet, data);
		packet.setStrength(packet.getStrength() * efficiency * 0.75D);
	}

	public double getEfficiency(AntennaNetworkPacket packet, EndFedAntennaData data) {
		int desiredLength = (int)Math.round(packet.getWavelength() / 2.0D); // The desired length is 1/2 of the wavelength used, round to nearest int.
		int incorrectBlocks = (int)Math.abs(desiredLength - data.length);

		return incorrectBlocks == 0 ? 1.0D : Math.pow(0.75D, incorrectBlocks);
	}

	@Override
	public EndFedAntennaData getDefaultData() {
		return new EndFedAntennaData(0);
	}


	public static class EndFedAntennaData extends AntennaData {

		private double length;

		public EndFedAntennaData(double length) {
			this.length = length;
		}

		public double getLength() {
			return length;
		}

		@Override
		public CompoundTag serializeNBT() {
			CompoundTag nbt = new CompoundTag();
			nbt.putDouble("length", length);
			return nbt;
		}

		@Override
		public void deserializeNBT(CompoundTag nbt) {
			length = nbt.getDouble("length");
		}

	}
}
