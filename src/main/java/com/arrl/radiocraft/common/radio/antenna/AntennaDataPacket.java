package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import net.minecraft.server.level.ServerLevel;

/** A {@link TncPacket} on its way between antennas, with the strength it arrives at. */
public class AntennaDataPacket implements IAntennaPacket {

	private final ServerLevel level;
	private final TncPacket payload;
	private final Band band;
	private final float frequency;
	private double strength;
	private final IAntenna source;
	private IAntenna destination = null; // Set by the transmitting antenna for each receiver, for terrain.

	public AntennaDataPacket(ServerLevel level, TncPacket payload, Band band, float frequency, double strength, IAntenna source) {
		this.level = level;
		this.payload = payload;
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

	public void setStrength(double strength) {
		this.strength = strength;
	}

	@Override
	public IAntenna getDestination() {
		return destination;
	}

	public void setDestination(IAntenna destination) {
		this.destination = destination;
	}

	@Override
	public IAntenna getSource() {
		return source;
	}

	@Override
	public ServerLevel getLevel() {
		return level;
	}

	public TncPacket getPayload() {
		return payload;
	}
}
