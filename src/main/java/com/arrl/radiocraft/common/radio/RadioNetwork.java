package com.arrl.radiocraft.common.radio;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class RadioNetwork {

	private final Map<BlockPos, Radio> radios = new HashMap<>();

	public void putRadio(BlockPos pos, Radio radio) {
		for(BlockPos _pos : radios.keySet()) {
			Radio _radio = radios.get(_pos); // All radios connect to each other perfectly for testing.
			_radio.getConnections().put(pos, 1);
			if(!_pos.equals(pos))
				radio.getConnections().put(_pos, 1); // Do not add to self
		}

		radios.put(pos, radio);
	}

	public void removeRadio(BlockPos pos) {
		radios.remove(pos);
		for(Radio radio : radios.values())
			radio.getConnections().remove(pos); // Remove from all connections
	}

	public Radio getRadio(BlockPos pos) {
		return radios.get(pos);
	}

	public Map<BlockPos, Radio> allRadios() {
		return radios;
	}


}
