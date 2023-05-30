package com.arrl.radiocraft.api.capabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public interface IAntennaWireHolderCapability extends INBTSerializable<CompoundTag> {
	BlockPos getHeldPos();
	void setHeldPos(BlockPos pos);
}
