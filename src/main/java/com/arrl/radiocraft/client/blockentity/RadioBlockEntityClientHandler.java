package com.arrl.radiocraft.client.blockentity;

import com.arrl.radiocraft.common.blockentities.RadioBlockEntity;
import com.arrl.radiocraft.common.sounds.RadioMorseSoundInstance;
import com.arrl.radiocraft.common.sounds.RadioStaticSoundInstance;
import net.minecraft.client.Minecraft;

public class RadioBlockEntityClientHandler {

	public static void startStaticSoundInstance(RadioBlockEntity radio) {
		Minecraft.getInstance().getSoundManager().play(new RadioStaticSoundInstance(radio));
	}

	public static void startMorseSoundInstance(RadioBlockEntity radio) {
		Minecraft.getInstance().getSoundManager().play(new RadioMorseSoundInstance(radio));
	}

}
