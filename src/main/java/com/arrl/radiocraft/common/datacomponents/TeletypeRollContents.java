package com.arrl.radiocraft.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * A roll torn off a teletype: its printed lines, oldest first, and how many line feeds it had been fed (the paper above
 * the first line), so the machine can take it back as it was.
 */
public record TeletypeRollContents(List<String> lines, int fedLines) {

	public static final TeletypeRollContents EMPTY = new TeletypeRollContents(List.of(), 0);

	public static final Codec<TeletypeRollContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.listOf().optionalFieldOf("lines", List.of()).forGetter(TeletypeRollContents::lines),
			Codec.INT.optionalFieldOf("fed", 0).forGetter(TeletypeRollContents::fedLines)
	).apply(instance, TeletypeRollContents::new));

	public static final StreamCodec<ByteBuf, TeletypeRollContents> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

	public boolean isEmpty() {
		return lines.isEmpty();
	}
}
