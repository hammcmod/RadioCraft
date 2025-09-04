package com.arrl.radiocraft.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record HandheldRadioState(boolean power, boolean ptt, int freq, float gain, float micGain, float receiveIndicatorStrength) {

    public static final Codec<HandheldRadioState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("power").forGetter(HandheldRadioState::power),
                    Codec.BOOL.fieldOf("ptt").forGetter(HandheldRadioState::ptt),
                    Codec.INT.fieldOf("frequency").forGetter(HandheldRadioState::freq),
                    Codec.FLOAT.fieldOf("gain").forGetter(HandheldRadioState::gain),
                    Codec.FLOAT.fieldOf("micGain").forGetter(HandheldRadioState::micGain)
            ).apply(instance, (power, ptt, freq, gain, micGain)->new HandheldRadioState(power, ptt, freq, gain, micGain, 0.0f)));

    public static final StreamCodec<ByteBuf, HandheldRadioState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, HandheldRadioState::power,
            ByteBufCodecs.BOOL, HandheldRadioState::ptt,
            ByteBufCodecs.INT, HandheldRadioState::freq,
            ByteBufCodecs.FLOAT, HandheldRadioState::gain,
            ByteBufCodecs.FLOAT, HandheldRadioState::micGain,
            ByteBufCodecs.FLOAT, HandheldRadioState::receiveIndicatorStrength,
            HandheldRadioState::new
    );

    public static HandheldRadioState getDefault() {
        return new HandheldRadioState(false, false, 146_520, 1.0f, 1.0f, 0.0f);
    }
}
