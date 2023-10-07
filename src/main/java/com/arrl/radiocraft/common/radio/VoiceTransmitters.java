package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.api.blockentities.radio.IVoiceTransmitter;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * A static registry containing the transmitters listening to voice packets within each level.
 */
public class VoiceTransmitters {

	public static final HashMap<Level, List<IVoiceTransmitter>> LISTENERS = new HashMap<>();

	public static void addListener(Level level, IVoiceTransmitter listener) {
		if(!LISTENERS.containsKey(level))
			LISTENERS.put(level, Collections.synchronizedList(new ArrayList<>())); // Use synchronized lists to make it thread safe, performance hit shouldn't matter as we aren't writing to this list very often.
		List<IVoiceTransmitter> listeners = LISTENERS.get(level);

		if(!listeners.contains(listener))
			listeners.add(listener);
	}

	public static void removeListener(Level level, IVoiceTransmitter listener) {
		if (!LISTENERS.containsKey(level))
			LISTENERS.put(level, Collections.synchronizedList(new ArrayList<>()));
		LISTENERS.get(level).remove(listener);
	}

}
