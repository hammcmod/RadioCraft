package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import com.arrl.radiocraft.common.radio.antenna.Antenna;
import com.arrl.radiocraft.common.radio.antenna.AntennaData;
import com.arrl.radiocraft.common.radio.antenna.types.DipoleAntennaType.DipoleAntennaData;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetworkPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

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
		if(level.getBlockState(pos).getBlock() != RadiocraftBlocks.BALUN_ONE_TO_ONE.get()) // Do not allow wires above or below, ensure center block is a 1:1 balun
			return null;
		if(level.getBlockState(pos.above()).getBlock() == RadiocraftBlocks.ANTENNA_WIRE.get())
			return null;
		if(level.getBlockState(pos.below()).getBlock() == RadiocraftBlocks.ANTENNA_WIRE.get())
			return null;

		for(Direction dir : Plane.HORIZONTAL) { // Check for arms
			BlockPos checkPos = pos.relative(dir);
			Block checkBlock = level.getBlockState(checkPos).getBlock();

			if(checkBlock == RadiocraftBlocks.ANTENNA_WIRE.get()) { // If wire is found in a horizontal direction.
				if(!checkSurroundingBlocks(level, pos, dir.getAxis())) // Make sure no other non-axis dir is connected;
					return null;

				int length1 = checkArmLength(level, pos, dir);
				if(length1 == -1) // Arm1 turned out to be invalid
					return null;
				else {
					int length2 = checkArmLength(level, pos, dir.getOpposite());
					if(length2 == -1) // Arm2 turned out to be invalid
						return null;
					else
						return new Antenna<>(this, pos, new DipoleAntennaData(length1, length2));
				}

			}
		}


		return null;
	}

	/**
	 * Check the length of an arm in a given direction, if an invalid block is found return -1.
	 */
	public int checkArmLength(Level level, BlockPos pos, Direction dir) {
		boolean stop = false;
		int length = 0;

		while(!stop) {
			BlockPos checkPos = pos.relative(dir, length + 1);
			if(level.getBlockState(checkPos).getBlock() == RadiocraftBlocks.ANTENNA_WIRE.get()) { // Next block is an antenna
				if(!checkSurroundingBlocks(level, checkPos, dir.getAxis()))
					return -1; // Return -1 if block is not valid
				else
					length++;
			}
			else {
				stop = true;
			}
		}

		return length;
	}

	public boolean checkSurroundingBlocks(Level level, BlockPos pos, Axis axis) {
		for(Direction dir : Direction.values()) {
			if(!axis.test(dir))
				if(RadiocraftTags.isAntennaBlock(level.getBlockState(pos.relative(dir)).getBlock()))
					return false;
		}
		return true;
	}

	@Override
	public DipoleAntennaData getDefaultData() {
		return new DipoleAntennaData(0, 0);
	}


	public static class DipoleAntennaData extends AntennaData {

		private int armLength1;
		private int armLength2;

		public DipoleAntennaData(int armLength1, int armLength2) {
			this.armLength1 = armLength1;
			this.armLength2 = armLength2;
		}

		public int getArmLength1() {
			return armLength1;
		}

		public int getArmLength2() {
			return armLength2;
		}

		@Override
		public CompoundTag serializeNBT() {
			CompoundTag nbt = new CompoundTag();
			nbt.putInt("armLength1", armLength1);
			nbt.putInt("armLength2", armLength2);
			return nbt;
		}

		@Override
		public void deserializeNBT(CompoundTag nbt) {
			armLength1 = nbt.getInt("armLength1");
			armLength2 = nbt.getInt("armLength2");
		}

	}
}
