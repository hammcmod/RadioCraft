package com.arrl.radiocraft.common.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * Which radio a {@link CordedMicrophoneItem} is plugged into, and which time it was taken: the radio remembers the
 * {@code micId} of the mic it lent out, so a mic from an earlier lending (left in a chest, say) no longer works.
 */
public record MicCord(BlockPos radio, UUID micId) {

    public static final Codec<MicCord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("radio").forGetter(MicCord::radio),
            UUIDUtil.CODEC.fieldOf("mic_id").forGetter(MicCord::micId)
    ).apply(instance, MicCord::new));

    public static final StreamCodec<ByteBuf, MicCord> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MicCord::radio,
            UUIDUtil.STREAM_CODEC, MicCord::micId,
            MicCord::new
    );
}
