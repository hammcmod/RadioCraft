package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.common.radio.antenna.Antenna;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AntennaNetwork  {

	private final Map<BlockPos, Antenna<?>> antennas = new ConcurrentHashMap<>(); // Concurrent map as it is read by the VoiP thread
	private final Level level;

	public AntennaNetwork(Level level) {
		this.level = level;
	}

	/**
	 * Do not call this from the VoiP thread
	 */
	public void addAntenna(BlockPos pos, Antenna<?> antenna) {
		antennas.put(pos, antenna);
	}

	/**
	 * Do not call this from the VoiP thread
	 */
	public void removeAntenna(BlockPos pos) {
		antennas.remove(pos);
	}

	public Antenna<?> getAntenna(BlockPos pos) {
		return antennas.get(pos);
	}

	public Map<BlockPos, Antenna<?>> allAntennas() {
		return antennas;
	}

	public Level getLevel() {
		return level;
	}

}