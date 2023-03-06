package com.arrl.radiocraft.common.radio;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class RadioNetwork {

	private final Map<BlockPos, Radio> radios = new HashMap<>();

	public void putRadio(BlockPos pos, Radio data) {
		radios.put(pos, data);
	}

	public void removeRadio(BlockPos pos) {
		radios.remove(pos);
	}

	public Radio getRadioData(BlockPos pos) {
		return radios.get(pos);
	}

	public Map<BlockPos, Radio> allRadios() {
		return radios;
	}


}
