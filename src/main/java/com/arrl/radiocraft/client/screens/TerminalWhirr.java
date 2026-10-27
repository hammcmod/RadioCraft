package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.common.init.RadiocraftSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * A terminal's fan and transformer humming while its screen is open (the H19, the direction finder, the microwave
 * radio): spins up over a second, stops when {@link #finish} is called from the screen's {@code removed}.
 */
public final class TerminalWhirr extends AbstractTickableSoundInstance {

	private final float loudness;

	private TerminalWhirr(float loudness) {
		super(RadiocraftSoundEvents.H19_WHIRR.get(), SoundSource.BLOCKS, RandomSource.create());
		this.loudness = loudness;
		looping = true;
		delay = 0;
		volume = 0.0F;
		relative = true;
		attenuation = Attenuation.NONE;
	}

	/** Starts a whirr at {@code loudness} (0 to 1). */
	public static TerminalWhirr start(float loudness) {
		TerminalWhirr whirr = new TerminalWhirr(loudness);
		Minecraft.getInstance().getSoundManager().play(whirr);
		return whirr;
	}

	@Override
	public void tick() {
		volume = Math.min(loudness, volume + loudness / 20.0F);
	}

	public void finish() {
		stop();
	}
}
