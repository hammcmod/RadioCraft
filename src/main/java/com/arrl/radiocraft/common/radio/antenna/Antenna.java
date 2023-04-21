package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.radio.voice.AntennaNetworkPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class Antenna<T> {

	public final IAntennaType<T> type;
	public final T data;
	public final Level level;
	public final BlockPos pos;

	public Antenna(IAntennaType<T> type, Level level, BlockPos pos, T data) {
		this.type = type;
		this.data = data;
		this.level = level;
		this.pos = pos;
	}

	public void processTransmit(AntennaNetworkPacket packet, BlockPos destination) {
		type.applyTransmitStrength(packet, data, pos, destination);
	}

	public void processReceive(AntennaNetworkPacket packet, BlockPos source) {
		type.applyReceiveStrength(packet, data, pos, source);
	}

}
