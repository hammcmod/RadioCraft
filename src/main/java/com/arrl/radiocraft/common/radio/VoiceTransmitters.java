package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.api.blockentities.radio.IVoiceTransmitter;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A static registry containing the transmitters listening to voice packets within each level.
 */
public class VoiceTransmitters {

	public static final ConcurrentHashMap<Level, List<IVoiceTransmitter>> LISTENERS = new ConcurrentHashMap<>();

	public static void addListener(Level level, IVoiceTransmitter listener) {
		List<IVoiceTransmitter> listeners = LISTENERS.computeIfAbsent(level, (l)->Collections.synchronizedList(new ArrayList<>())); // Use synchronized lists to make it thread safe, performance hit shouldn't matter as we aren't writing to this list very often.

		synchronized (listeners) {
			if (!listeners.contains(listener))
				listeners.add(listener);
		}
	}

	public static void removeListener(Level level, IVoiceTransmitter listener) {
		LISTENERS.computeIfAbsent(level, (l)->Collections.synchronizedList(new ArrayList<>())).remove(listener);
	}

}
