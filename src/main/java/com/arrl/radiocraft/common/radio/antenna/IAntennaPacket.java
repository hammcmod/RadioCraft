package com.arrl.radiocraft.common.radio.antenna;

import net.minecraft.core.BlockPos;

public interface IAntennaPacket {

	int getWavelength();

	int getFrequency();

	double getStrength();

	BlockPos getSource();

}
