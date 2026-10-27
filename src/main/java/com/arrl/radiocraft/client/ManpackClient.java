package com.arrl.radiocraft.client;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.datacomponents.ManpackState;
import com.arrl.radiocraft.common.init.RadiocraftSoundEvents;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import com.arrl.radiocraft.common.radio.morse.CWReceiveBuffer;
import com.arrl.radiocraft.common.radio.voice.ReceiverMix;
import com.arrl.radiocraft.common.radio.voice.handheld.ManpackRadio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;

/**
 * The HF manpack on this client's player: its static (the HF noise floor through the whip, ducked under a signal,
 * quiet while transmitting) and received Morse, both following the player, and the key that opens its panel.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID, value = Dist.CLIENT)
public final class ManpackClient {

	private ManpackClient() {}

	/** The whip's share of the static a good antenna would bring in. */
	private static final double WHIP_STATIC = 0.6D;

	private static float signal = 0.0F;
	private static boolean transmitting = false;
	private static final CWReceiveBuffer CW = new CWReceiveBuffer();
	private static StaticSound staticSound = null;
	private static MorseSound morseSound = null;

	public static void update(float signal, boolean transmitting) {
		ManpackClient.signal = signal;
		ManpackClient.transmitting = transmitting;
	}

	public static void receiveCw(List<CWBuffer> buffers, float strength) {
		for(CWBuffer buffer : buffers)
			CW.addToBuffer(buffer, strength);
	}

	public static float signal() {
		return signal;
	}

	public static boolean transmitting() {
		return transmitting;
	}

	/** The manpack the local player has to hand, empty if none. */
	public static ItemStack manpack() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player == null ? ItemStack.EMPTY : ManpackRadio.find(player);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if(mc.player == null)
			return;
		while(RadiocraftKeys.OPEN_MANPACK.consumeClick())
			if(!manpack().isEmpty() && mc.screen == null)
				com.arrl.radiocraft.client.screens.ManpackScreen.open();
		boolean on = !manpack().isEmpty() && ManpackRadio.stateOf(manpack()).on();
		if(on) {
			if(staticSound == null || !mc.getSoundManager().isActive(staticSound)) {
				staticSound = new StaticSound();
				mc.getSoundManager().play(staticSound);
			}
			if(morseSound == null || !mc.getSoundManager().isActive(morseSound)) {
				morseSound = new MorseSound();
				mc.getSoundManager().play(morseSound);
			}
		}
	}

	/** Follows the local player, as long as their manpack is on. */
	private abstract static class Following extends AbstractTickableSoundInstance {
		Following(net.minecraft.sounds.SoundEvent sound) {
			super(sound, SoundSource.VOICE, SoundInstance.createUnseededRandom());
			this.looping = true;
			this.delay = 0;
			this.volume = 0.0F;
			LocalPlayer player = Minecraft.getInstance().player;
			if(player != null) {
				x = player.getX();
				y = player.getEyeY();
				z = player.getZ();
			}
		}

		@Override
		public void tick() {
			LocalPlayer player = Minecraft.getInstance().player;
			ItemStack stack = manpack();
			if(player == null || stack.isEmpty() || !ManpackRadio.stateOf(stack).on()) {
				stop();
				return;
			}
			x = player.getX();
			y = player.getEyeY();
			z = player.getZ();
			volume = volumeFor(ManpackRadio.stateOf(stack));
		}

		abstract float volumeFor(ManpackState state);

		@Override
		public boolean canStartSilent() {
			return true;
		}
	}

	private static final class StaticSound extends Following {
		private double duck = 1.0D;

		StaticSound() {
			super(RadiocraftSoundEvents.STATIC.get());
		}

		@Override
		float volumeFor(ManpackState state) {
			if(transmitting || state.cw())
				return 0.0F;
			double noise = Math.min(1.0D, RadiocraftClientValues.NOISE_VOLUME + (RadiocraftClientValues.AURORA_ACTIVE ? RadiocraftClientValues.AURORA_NOISE : 0.0D));
			double wanted = ReceiverMix.noiseDuck(signal, noise);
			duck += (wanted - duck) * (wanted < duck ? 0.6D : 0.15D);
			return (float)(noise * duck * WHIP_STATIC * state.gain());
		}
	}

	private static final class MorseSound extends Following {
		MorseSound() {
			super(RadiocraftSoundEvents.MORSE.get());
		}

		@Override
		float volumeFor(ManpackState state) {
			return state.cw() ? CW.getNextStrength() * state.gain() : 0.0F;
		}
	}
}
