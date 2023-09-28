package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import net.minecraft.server.level.ServerLevel;

public class AntennaMorsePacket implements IAntennaPacket {

	private final ServerLevel level;
	private final int wavelength;
	private final int frequency;
	private double strength;
	private final IAntenna source;

	public AntennaMorsePacket(ServerLevel level, int wavelength, int frequency, double strength, IAntenna source) {
		this.level = level;
		this.wavelength = wavelength;
		this.frequency = frequency;
		this.strength = strength;
		this.source = source;
	}

	@Override
	public int getWavelength() {
		return wavelength;
	}

	@Override
	public int getFrequency() {
		return frequency;
	}

	@Override
	public double getStrength() {
		return strength;
	}

	@Override
	public IAntenna getSource() {
		return source;
	}

	public ServerLevel getLevel() {
		return level;
	}

	public void setStrength(double strength) {
		this.strength = strength;
	}


}
