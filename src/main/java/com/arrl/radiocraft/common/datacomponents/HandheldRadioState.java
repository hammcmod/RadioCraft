package com.arrl.radiocraft.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * @param ctcssTone CTCSS tone sent under the voice and needed to open the squelch, 0 for none.
 * @param offsetHz  Repeater offset: the radio transmits this far from its frequency (0 for simplex).
 */
public record HandheldRadioState(boolean power, boolean ptt, float freq, float gain, float micGain, float receiveIndicatorStrength, float ctcssTone, int offsetHz) {

    public HandheldRadioState(boolean power, boolean ptt, float freq, float gain, float micGain, float receiveIndicatorStrength) {
        this(power, ptt, freq, gain, micGain, receiveIndicatorStrength, 0.0f, 0);
    }

    public static final Codec<HandheldRadioState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("power").forGetter(HandheldRadioState::power),
                    Codec.BOOL.fieldOf("ptt").forGetter(HandheldRadioState::ptt),
                    Codec.FLOAT.fieldOf("frequency").forGetter(HandheldRadioState::freq),
                    Codec.FLOAT.fieldOf("gain").forGetter(HandheldRadioState::gain),
                    Codec.FLOAT.fieldOf("micGain").forGetter(HandheldRadioState::micGain),
                    Codec.FLOAT.optionalFieldOf("ctcssTone", 0.0f).forGetter(HandheldRadioState::ctcssTone),
                    Codec.INT.optionalFieldOf("offsetHz", 0).forGetter(HandheldRadioState::offsetHz)
            ).apply(instance, (power, ptt, freq, gain, micGain, tone, offset)->new HandheldRadioState(power, ptt, freq, gain, micGain, 0.0f, tone, offset)));

    public static final StreamCodec<ByteBuf, HandheldRadioState> STREAM_CODEC = StreamCodec.of(
            (buf, state) -> {
                buf.writeBoolean(state.power());
                buf.writeBoolean(state.ptt());
                buf.writeFloat(state.freq());
                buf.writeFloat(state.gain());
                buf.writeFloat(state.micGain());
                buf.writeFloat(state.receiveIndicatorStrength());
                buf.writeFloat(state.ctcssTone());
                buf.writeInt(state.offsetHz());
            },
            buf -> new HandheldRadioState(buf.readBoolean(), buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readInt())
    );

    public static HandheldRadioState getDefault() {
        return new HandheldRadioState(false, false, 146_520_000.0f, 1.0f, 1.0f, 0.0f);
    }

    /** This state with its tone and offset replaced. */
    public HandheldRadioState withToneAndOffset(float tone, int offset) {
        return new HandheldRadioState(power, ptt, freq, gain, micGain, receiveIndicatorStrength, tone, offset);
    }
}
