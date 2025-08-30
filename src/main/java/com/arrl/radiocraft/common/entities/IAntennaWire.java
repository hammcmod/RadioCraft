package com.arrl.radiocraft.common.entities;

import net.minecraft.core.BlockPos;

public interface IAntennaWire {
	BlockPos getEndPos();
	BlockPos getStartPos();
	double getLength();
	boolean isPairedWith(IAntennaWire other);
	void updateAntennas();
}
