package com.arrl.radiocraft.common.sounds;

import com.arrl.radiocraft.client.RadiocraftClientValues;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftSoundEvents;
import com.arrl.radiocraft.common.radio.voice.ReceiverMix;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public class RadioStaticSoundInstance extends AbstractTickableSoundInstance {

	/** Share of the way the static moves toward its ducked level each tick, going down and coming back up. */
	private static final double DUCK_ATTACK = 0.6D;
	private static final double DUCK_RELEASE = 0.15D;

	private final RadioBlockEntity radio;
	private double duck = 1.0D;

	public RadioStaticSoundInstance(RadioBlockEntity radio) {
		super(RadiocraftSoundEvents.STATIC.get(), SoundSource.VOICE, SoundInstance.createUnseededRandom());
		this.radio = radio;
		this.looping = true;
		this.delay = 0;
		this.x = radio.getBlockPos().getX() + 0.5D;
		this.y = radio.getBlockPos().getY() + 0.5D;
		this.z = radio.getBlockPos().getZ() + 0.5D;
		this.volume = 1.0F;
	}

	@Override
	public void tick() {
		if(radio.isRemoved())
			stop();
		else {
			if(radio.wasPowered() && radio.shouldPlayStatic() && !radio.isReceiverMuted()) {
				// Solar noise floor, plus whatever redstone is switching near the station and any aurora overhead, through the antenna match.
				double noise = Math.min(1.0D, RadiocraftClientValues.NOISE_VOLUME + radio.getInterference() + auroraNoise());
				// The receiver's AGC: a signal well above the noise pushes the static down, fast in and slower back out.
				double wanted = ReceiverMix.noiseDuck(radio.getSyncedSignal(), noise);
				duck += (wanted - duck) * (wanted < duck ? DUCK_ATTACK : DUCK_RELEASE);
				volume = (float)(noise * duck * radio.getStaticVolume());
			}
			else
				volume = 0.0F;
		}
	}

	/** Extra static while an aurora is lit over this radio. */
	private double auroraNoise() {
		boolean underAurora = RadiocraftClientValues.AURORA_ACTIVE && Math.abs((long)radio.getBlockPos().getZ()) >= RadiocraftClientValues.AURORA_LATITUDE;
		return underAurora ? RadiocraftClientValues.AURORA_NOISE : 0.0D;
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}

}
