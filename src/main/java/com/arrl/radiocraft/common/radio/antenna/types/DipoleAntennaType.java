package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import com.arrl.radiocraft.common.radio.antenna.Antenna;
import com.arrl.radiocraft.common.radio.antenna.types.DipoleAntennaType.DipoleAntennaData;
import com.arrl.radiocraft.common.radio.voice.AntennaNetworkPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
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
	public void applyTransmitStrength(AntennaNetworkPacket packet, DipoleAntennaData data, BlockPos pos, BlockPos destination) {

	}

	@Override
	public void applyReceiveStrength(AntennaNetworkPacket packet, DipoleAntennaData data, BlockPos pos, BlockPos source) {

	}

	@Override
	public Antenna<DipoleAntennaData> match(Level level, BlockPos pos) {
		if(level.getBlockState(pos).getBlock() != RadiocraftBlocks.BALUN_ONE_TO_ONE.get()) // Do not allow wires above or below, ensure center block is a 1:1 balun
			return null;
		if(level.getBlockState(pos.above()).getBlock() == RadiocraftBlocks.ANTENNA_CONNECTOR.get())
			return null;
		if(level.getBlockState(pos.below()).getBlock() == RadiocraftBlocks.ANTENNA_CONNECTOR.get())
			return null;

		for(Direction dir : Plane.HORIZONTAL) { // Check for arms
			BlockPos checkPos = pos.relative(dir);
			Block checkBlock = level.getBlockState(checkPos).getBlock();

			if(checkBlock == RadiocraftBlocks.ANTENNA_CONNECTOR.get()) { // If wire is found in a horizontal direction.
				if(!checkSurroundingBlocks(level, pos, dir.getAxis())) // Make sure no other non-axis dir is connected;
					return null;

				int length1 = checkArmLength(level, checkPos, dir);
				if(length1 == -1) // Arm1 turned out to be invalid
					return null;
				else {
					int length2 = checkArmLength(level, checkPos, dir.getOpposite());
					if(length2 == -1) // Arm2 turned out to be invalid
						return null;
					else
						return new Antenna<>(this, level, pos, new DipoleAntennaData(length1, length2));
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
			if(level.getBlockState(checkPos).getBlock() == RadiocraftBlocks.ANTENNA_CONNECTOR.get()) { // Next block is an antenna
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


	public record DipoleAntennaData(int armLength1, int armLength2) {}
}
