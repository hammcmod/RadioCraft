package com.arrl.radiocraft.common.sounds;

import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftSoundEvents;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * The buzz a radio makes when something keeps switching near its antenna: silent for the odd lever flip, fading in once
 * the redstone interference level stays up (a clock, a piston loop). Plays on top of the static, which also rises.
 * <p>
 * Placeholder audio: the morse tone pitched down to a low hum. A proper mains-style buzz loop is on the asset list in
 * {@code doc/ModelDesignBounty.md}; swapping it in is a one-line change here.
 */
public class RadioInterferenceHumSoundInstance extends AbstractTickableSoundInstance {

	/** Interference level below which the hum stays silent, so single pops only show in the static. */
	public static final double THRESHOLD = 0.25D;
	private static final float MAX_VOLUME = 0.6F;

	private final RadioBlockEntity radio;

	public RadioInterferenceHumSoundInstance(RadioBlockEntity radio) {
		super(RadiocraftSoundEvents.MORSE.get(), SoundSource.VOICE, SoundInstance.createUnseededRandom());
		this.radio = radio;
		this.looping = true;
		this.delay = 0;
		this.pitch = 0.5F;
		this.x = radio.getBlockPos().getX() + 0.5D;
		this.y = radio.getBlockPos().getY() + 0.5D;
		this.z = radio.getBlockPos().getZ() + 0.5D;
		this.volume = 0.0F;
	}

	@Override
	public void tick() {
		if(radio.isRemoved()) {
			stop();
			return;
		}
		if(radio.wasPowered() && radio.shouldPlayStatic() && !radio.isReceiverMuted()) {
			double sustained = Math.max(0.0D, radio.getInterference() - THRESHOLD) / (1.0D - THRESHOLD);
			volume = (float)(sustained * MAX_VOLUME * radio.getStaticVolume());
		}
		else
			volume = 0.0F;
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}
}
