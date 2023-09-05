package com.arrl.radiocraft.api.capabilities;

import net.minecraft.core.BlockPos;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public interface IAntennaWireHolderCapability {
	BlockPos getHeldPos();
	void setHeldPos(BlockPos pos);
	boolean hasHeldWire();
}
