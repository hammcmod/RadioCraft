package com.arrl.radiocraft.common.sounds;

import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftSoundEvents;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * The noise a radio makes when redstone switches near its antenna: the maintainer's {@code redstone_rfi.ogg} (2026-10-27,
 * 19 s of broadband switching hash), played at the radio with its loudness following the interference level. A single
 * lever flip is a short burst that dies away with the level (a second or so of the file); a clock or a piston loop keeps
 * it going, looping the file. Plays on top of the static, which also rises. Only redstone drives it: lightning static
 * feeds the same meter and had it grunting through a whole storm (2026-10-27), so it follows the redstone part alone.
 */
public class RadioInterferenceHumSoundInstance extends AbstractTickableSoundInstance {

	/** Redstone interference level below which the noise is off: just under a single pop's tail, so a flip is heard and then stops. */
	public static final double THRESHOLD = 0.06D;
	private static final float MAX_VOLUME = 0.8F;

	private final RadioBlockEntity radio;

	public RadioInterferenceHumSoundInstance(RadioBlockEntity radio) {
		super(RadiocraftSoundEvents.REDSTONE_RFI.get(), SoundSource.VOICE, SoundInstance.createUnseededRandom());
		this.radio = radio;
		this.looping = true;
		this.delay = 0;
		this.pitch = 1.0F;
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
			double level = radio.getRedstoneInterference(); // Redstone only: lightning static is the static's business.
			double heard = level < THRESHOLD ? 0.0D : Math.min(1.0D, Math.sqrt(level)); // Loud quickly, like switching hash does.
			volume = (float)(heard * MAX_VOLUME * radio.getStaticVolume());
		}
		else
			volume = 0.0F;
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}
}
