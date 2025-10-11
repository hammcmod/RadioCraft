package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;

public class AntennaCWPacket implements IAntennaPacket {

	private final ServerLevel level;
	private final Collection<CWBuffer> buffers;
	private final Band band;
	private final float frequency;
	private double strength;
	private final IAntenna source;

	public AntennaCWPacket(ServerLevel level, Collection<CWBuffer> buffers, Band band, float frequency, double strength, IAntenna source) {
		this.level = level;
		this.buffers = buffers;
		this.band = band;
		this.frequency = frequency;
		this.strength = strength;
		this.source = source;
	}

	@Override
	public Band getBand() {
		return band;
	}

	@Override
	public float getFrequency() {
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

	@Override
	public ServerLevel getLevel() {
		return level;
	}

	public Collection<CWBuffer> getBuffers() {
		return buffers;
	}

	public void setStrength(double strength) {
		this.strength = strength;
	}


}
