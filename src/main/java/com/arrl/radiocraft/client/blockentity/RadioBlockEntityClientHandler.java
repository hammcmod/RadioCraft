package com.arrl.radiocraft.client.blockentity;

import com.arrl.radiocraft.common.blockentities.radio.HFRadioBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.sounds.RadioInterferenceHumSoundInstance;
import com.arrl.radiocraft.common.sounds.RadioMorseSoundInstance;
import com.arrl.radiocraft.common.sounds.RadioStaticSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RadioBlockEntityClientHandler {

	/**
	 * Every looping sound a loaded radio owns. Each is started once, when the radio loads, but the sound engine can drop
	 * a looping sound for good: the audio device changing (headphones, the Windows default output), a resource reload,
	 * the Voice/Speech slider touching 0, or a full channel pool. {@link #keepSoundsPlaying} starts those again.
	 */
	private static final Map<RadioBlockEntity, List<SoundInstance>> SOUNDS = new HashMap<>();

	public static void startStaticSoundInstance(RadioBlockEntity radio) {
		play(radio, new RadioStaticSoundInstance(radio));
	}

	public static void startMorseSoundInstance(HFRadioBlockEntity radio) {
		play(radio, new RadioMorseSoundInstance(radio));
	}

	public static void startInterferenceHumSoundInstance(RadioBlockEntity radio) {
		play(radio, new RadioInterferenceHumSoundInstance(radio));
	}

	private static void play(RadioBlockEntity radio, SoundInstance sound) {
		SOUNDS.computeIfAbsent(radio, r -> new ArrayList<>()).add(sound);
		Minecraft.getInstance().getSoundManager().play(sound);
	}

	/**
	 * A radio that plays static plays it through each of its wired speakers too: one loop per speaker, started when the
	 * speaker turns up and dropped when it goes.
	 */
	private static void syncSpeakerStatic(RadioBlockEntity radio, List<SoundInstance> sounds) {
		if(sounds.stream().noneMatch(sound -> sound instanceof RadioStaticSoundInstance stat && stat.getSpeaker() == null))
			return; // This radio has no static of its own.
		sounds.removeIf(sound -> sound instanceof RadioStaticSoundInstance stat && stat.getSpeaker() != null && !radio.getWiredSpeakers().contains(stat.getSpeaker()));
		for(com.arrl.radiocraft.common.radio.BEVoiceReceiver.WiredSpeaker speaker : radio.getWiredSpeakers())
			if(sounds.stream().noneMatch(sound -> sound instanceof RadioStaticSoundInstance stat && speaker.equals(stat.getSpeaker()))) {
				RadioStaticSoundInstance sound = new RadioStaticSoundInstance(radio, speaker);
				sounds.add(sound);
				Minecraft.getInstance().getSoundManager().play(sound);
			}
	}

	/** Restarts any of the radio's sounds the engine dropped, and forgets radios that are gone. */
	public static void keepSoundsPlaying(RadioBlockEntity radio) {
		Minecraft mc = Minecraft.getInstance();
		SOUNDS.keySet().removeIf(r -> r.isRemoved() || r.getLevel() != mc.level); // The sounds hold their radio, so drop them by hand.

		List<SoundInstance> sounds = SOUNDS.get(radio);
		if(sounds == null)
			return;
		syncSpeakerStatic(radio, sounds);
		SoundManager soundManager = mc.getSoundManager();
		for(SoundInstance sound : sounds) {
			if(!soundManager.isActive(sound))
				soundManager.play(sound);
		}
	}

}
