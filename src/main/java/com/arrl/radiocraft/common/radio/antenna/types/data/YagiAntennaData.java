package com.arrl.radiocraft.common.radio.antenna.types.data;

import com.arrl.radiocraft.common.radio.antenna.AntennaData;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

public class YagiAntennaData extends AntennaData {

	private Direction facing;

	public YagiAntennaData(Direction facing) {
		this.facing = facing;
	}

	public Direction getFacing() {
		return facing;
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag nbt = new CompoundTag();
		nbt.putInt("facing", facing.ordinal());
		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		facing = Direction.values()[nbt.getInt("facing")];
	}

}