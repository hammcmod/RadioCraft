package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.common.blockentities.AbstractRadioBlockEntity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class RadioManager {

	public static final HashMap<Level, List<AbstractRadioBlockEntity>> RADIOS = new HashMap<>();

	public static void addRadio(Level level, AbstractRadioBlockEntity radio) {
		if(!RADIOS.containsKey(level))
			RADIOS.put(level, Collections.synchronizedList(new ArrayList<>())); // Use synchronized lists to make it thread safe, performance hit shouldn't matter as we aren't writing to this list very often.
		List<AbstractRadioBlockEntity> radios = RADIOS.get(level);

		if(!radios.contains(radio))
			radios.add(radio);
	}

	public static void removeRadio(Level level, AbstractRadioBlockEntity radio) {
		if(!RADIOS.containsKey(level))
			RADIOS.put(level, Collections.synchronizedList(new ArrayList<>()));
		RADIOS.get(level).remove(radio);
	}

}
