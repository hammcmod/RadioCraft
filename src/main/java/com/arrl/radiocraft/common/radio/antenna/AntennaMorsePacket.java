package com.arrl.radiocraft.common.radio.antenna;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class AntennaMorsePacket implements IAntennaPacket {

	private final ServerLevel level;
	private final int wavelength;
	private final int frequency;
	private double strength;
	private final BlockPos source;

	public AntennaMorsePacket(ServerLevel level, int wavelength, int frequency, double strength, BlockPos source) {
		this.level = level;
		this.wavelength = wavelength;
		this.frequency = frequency;
		this.strength = strength;
		this.source = source;
	}

	public ServerLevel getLevel() {
		return level;
	}

	public int getWavelength() {
		return wavelength;
	}

	public int getFrequency() {
		return frequency;
	}

	public double getStrength() {
		return strength;
	}

	public void setStrength(double strength) {
		this.strength = strength;
	}

	public BlockPos getSource() {
		return source;
	}

}
