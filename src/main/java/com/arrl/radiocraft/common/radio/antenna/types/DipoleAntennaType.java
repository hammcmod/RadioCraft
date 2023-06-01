package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.antenna.Antenna;
import com.arrl.radiocraft.common.radio.antenna.AntennaData;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetworkPacket;
import com.arrl.radiocraft.common.radio.antenna.types.DipoleAntennaType.DipoleAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class DipoleAntennaType implements IAntennaType<DipoleAntennaData> {

	public static final ResourceLocation ID = Radiocraft.location("dipole");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public void applyTransmitStrength(AntennaNetworkPacket packet, DipoleAntennaData data, BlockPos destination) {

	}

	@Override
	public void applyReceiveStrength(AntennaNetworkPacket packet, DipoleAntennaData data, BlockPos pos) {

	}

	@Override
	public Antenna<DipoleAntennaData> match(Level level, BlockPos pos) {
		if(level.getBlockState(pos).getBlock() != RadiocraftBlocks.BALUN_ONE_TO_ONE.get())
			return null; // Do not match if center block is not a 1:1 balun.

		List<BlockPos> endPoints = new ArrayList<>();

		AntennaWire.getAntennaWires(level, pos).forEach(wire -> endPoints.add(wire.getEndPos())); // Grab ends and starts separately to avoid needing to check if each pos is the start or end of the entity.
		AntennaWire.getAntennaWireParts(level, pos).forEach(part -> endPoints.add(part.parent.blockPosition()));

		if(endPoints.size() != 2)
			return null; // Do not match if there are not 2 connections to the center block.

		BlockPos arm1 = endPoints.get(0);
		BlockPos arm2 = endPoints.get(1);

		if(AntennaWire.getWires(level, arm1).size() > 1 || AntennaWire.getWires(level, arm2).size() > 1)
			return null; // Do not match if either arm continues.

		BlockPos relativeArm1 = arm1.subtract(pos);
		BlockPos relativeArm2 = arm2.subtract(pos);
		Vec3 directionArm1 = new Vec3(relativeArm1.getX(), 0.0D, relativeArm1.getZ()).normalize();
		Vec3 directionArm2 = new Vec3(relativeArm2.getX(), 0.0D, relativeArm2.getZ()).normalize();

		Radiocraft.LOGGER.info(String.valueOf(directionArm1.dot(directionArm2)));

		return null;
	}

	@Override
	public DipoleAntennaData getDefaultData() {
		return new DipoleAntennaData(0, 0);
	}


	public static class DipoleAntennaData extends AntennaData {

		private double armLength1;
		private double armLength2;

		public DipoleAntennaData(int armLength1, int armLength2) {
			this.armLength1 = armLength1;
			this.armLength2 = armLength2;
		}

		public double getArmLength1() {
			return armLength1;
		}

		public double getArmLength2() {
			return armLength2;
		}

		@Override
		public CompoundTag serializeNBT() {
			CompoundTag nbt = new CompoundTag();
			nbt.putDouble("armLength1", armLength1);
			nbt.putDouble("armLength2", armLength2);
			return nbt;
		}

		@Override
		public void deserializeNBT(CompoundTag nbt) {
			armLength1 = nbt.getDouble("armLength1");
			armLength2 = nbt.getDouble("armLength2");
		}

	}
}
