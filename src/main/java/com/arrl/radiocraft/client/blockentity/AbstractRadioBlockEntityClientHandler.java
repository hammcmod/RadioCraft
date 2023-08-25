package com.arrl.radiocraft.client.blockentity;

import com.arrl.radiocraft.common.blockentities.AbstractRadioBlockEntity;
import com.arrl.radiocraft.common.sounds.RadioMorseSoundInstance;
import com.arrl.radiocraft.common.sounds.RadioStaticSoundInstance;
import net.minecraft.client.Minecraft;

public class AbstractRadioBlockEntityClientHandler {

	public static void startSoundInstances(AbstractRadioBlockEntity radio) {
		Minecraft.getInstance().getSoundManager().play(new RadioStaticSoundInstance(radio));
		Minecraft.getInstance().getSoundManager().play(new RadioMorseSoundInstance(radio));
	}

}
