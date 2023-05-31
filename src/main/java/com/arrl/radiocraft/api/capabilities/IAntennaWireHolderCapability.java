package com.arrl.radiocraft.api.capabilities;

import net.minecraft.core.BlockPos;

public interface IAntennaWireHolderCapability {
	BlockPos getHeldPos();
	void setHeldPos(BlockPos pos);
	boolean hasHeldWire();
}
