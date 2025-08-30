package com.arrl.radiocraft.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record HandheldRadioState(boolean power, boolean ptt, int freq, float receiveIndicatorStrength) {

    public static final Codec<HandheldRadioState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("power").forGetter(HandheldRadioState::power),
                    Codec.BOOL.fieldOf("ptt").forGetter(HandheldRadioState::ptt),
                    Codec.INT.fieldOf("frequency").forGetter(HandheldRadioState::freq)
            ).apply(instance, (power, ptt, freq)->new HandheldRadioState(power, ptt, freq, 0)));

    public static final StreamCodec<ByteBuf, HandheldRadioState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, HandheldRadioState::power,
            ByteBufCodecs.BOOL, HandheldRadioState::ptt,
            ByteBufCodecs.INT, HandheldRadioState::freq,
            ByteBufCodecs.FLOAT, HandheldRadioState::receiveIndicatorStrength,
            HandheldRadioState::new
    );

    public static HandheldRadioState getDefault() {
        return new HandheldRadioState(false, false, 146_520, 0);
    }
}
