package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.api.capabilities.IAntennaWireHolderCapability;
import net.minecraft.core.BlockPos;

public class AntennaWireHolderCapability implements IAntennaWireHolderCapability {

	public BlockPos heldPos = null;

	@Override
	public BlockPos getHeldPos() {
		return heldPos;
	}

	@Override
	public void setHeldPos(BlockPos pos) {
		heldPos = pos;
	}

	@Override
	public boolean hasHeldWire() {
		return heldPos != null;
	}

}
