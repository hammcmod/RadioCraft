package com.arrl.radiocraft.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record HandheldRadioState(boolean power, boolean ptt, float freq, float gain, float micGain, float receiveIndicatorStrength, boolean vox) {

    public static final Codec<HandheldRadioState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("power").forGetter(HandheldRadioState::power),
                    Codec.BOOL.fieldOf("ptt").forGetter(HandheldRadioState::ptt),
                    Codec.FLOAT.fieldOf("frequency").forGetter(HandheldRadioState::freq),
                    Codec.FLOAT.fieldOf("gain").forGetter(HandheldRadioState::gain),
                    Codec.FLOAT.fieldOf("micGain").forGetter(HandheldRadioState::micGain),
                    Codec.BOOL.fieldOf("vox").forGetter(HandheldRadioState::vox)
            ).apply(instance, (power, ptt, freq, gain, micGain, vox)->new HandheldRadioState(power, ptt, freq, gain, micGain, 0.0f, vox)));

    public static final StreamCodec<ByteBuf, HandheldRadioState> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public HandheldRadioState decode(ByteBuf buf) {
            boolean power = ByteBufCodecs.BOOL.decode(buf);
            boolean ptt = ByteBufCodecs.BOOL.decode(buf);
            float freq = ByteBufCodecs.FLOAT.decode(buf);
            float gain = ByteBufCodecs.FLOAT.decode(buf);
            float micGain = ByteBufCodecs.FLOAT.decode(buf);
            float receiveIndicatorStrength = ByteBufCodecs.FLOAT.decode(buf);
            boolean vox = ByteBufCodecs.BOOL.decode(buf);
            return new HandheldRadioState(power, ptt, freq, gain, micGain, receiveIndicatorStrength, vox);
        }

        @Override
        public void encode(ByteBuf buf, HandheldRadioState state) {
            ByteBufCodecs.BOOL.encode(buf, state.power());
            ByteBufCodecs.BOOL.encode(buf, state.ptt());
            ByteBufCodecs.FLOAT.encode(buf, state.freq());
            ByteBufCodecs.FLOAT.encode(buf, state.gain());
            ByteBufCodecs.FLOAT.encode(buf, state.micGain());
            ByteBufCodecs.FLOAT.encode(buf, state.receiveIndicatorStrength());
            ByteBufCodecs.BOOL.encode(buf, state.vox());
        }
    };

    public static HandheldRadioState getDefault() {
        return new HandheldRadioState(false, false, 146_520_000.0f, 1.0f, 1.0f, 0.0f, false);
    }
}
