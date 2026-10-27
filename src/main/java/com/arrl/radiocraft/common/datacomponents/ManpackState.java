package com.arrl.radiocraft.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The HF manpack's settings, kept on the item: switched on, which band (10m or 20m), the dial frequency, CW or SSB, the
 * AF and mic gain, the battery's energy, and whether its handset is out on the cord.
 */
public record ManpackState(boolean on, boolean tenMetres, float frequency, boolean cw, float gain, float micGain, int energy, boolean micOut) {

	/** Energy a full manpack battery holds: about four Minecraft days listening. */
	public static final int CAPACITY = 120_000;

	public static final float TEN_METRE_BOTTOM = 28_000_000.0F, TWENTY_METRE_BOTTOM = 14_000_000.0F;

	public static final Codec<ManpackState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("on").forGetter(ManpackState::on),
			Codec.BOOL.fieldOf("ten_metres").forGetter(ManpackState::tenMetres),
			Codec.FLOAT.fieldOf("frequency").forGetter(ManpackState::frequency),
			Codec.BOOL.fieldOf("cw").forGetter(ManpackState::cw),
			Codec.FLOAT.fieldOf("gain").forGetter(ManpackState::gain),
			Codec.FLOAT.fieldOf("mic_gain").forGetter(ManpackState::micGain),
			Codec.INT.fieldOf("energy").forGetter(ManpackState::energy),
			Codec.BOOL.fieldOf("mic_out").forGetter(ManpackState::micOut)
	).apply(instance, ManpackState::new));

	public static final StreamCodec<ByteBuf, ManpackState> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

	/** Fresh off the bench: off, on 20m, full battery. */
	public static ManpackState fresh(int capacity) {
		return new ManpackState(false, false, 14_200_000.0F, false, 1.0F, 1.0F, capacity, false);
	}

	public String bandName() {
		return tenMetres ? "10m" : "20m";
	}

	public ManpackState withOn(boolean value) {
		return new ManpackState(value, tenMetres, frequency, cw, gain, micGain, energy, micOut);
	}

	/** The other band, at its bottom edge... or rather its usual phone calling area, a little up. */
	public ManpackState withBand(boolean ten) {
		return new ManpackState(on, ten, ten ? TEN_METRE_BOTTOM : TWENTY_METRE_BOTTOM, cw, gain, micGain, energy, micOut);
	}

	public ManpackState withFrequency(float value) {
		return new ManpackState(on, tenMetres, value, cw, gain, micGain, energy, micOut);
	}

	public ManpackState withCw(boolean value) {
		return new ManpackState(on, tenMetres, frequency, value, gain, micGain, energy, micOut);
	}

	public ManpackState withGain(float value) {
		return new ManpackState(on, tenMetres, frequency, cw, value, micGain, energy, micOut);
	}

	public ManpackState withMicGain(float value) {
		return new ManpackState(on, tenMetres, frequency, cw, gain, value, energy, micOut);
	}

	public ManpackState withEnergy(int value) {
		return new ManpackState(on, tenMetres, frequency, cw, gain, micGain, value, micOut);
	}

	public ManpackState withMicOut(boolean value) {
		return new ManpackState(on, tenMetres, frequency, cw, gain, micGain, energy, value);
	}
}
