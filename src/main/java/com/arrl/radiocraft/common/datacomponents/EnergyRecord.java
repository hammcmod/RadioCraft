package com.arrl.radiocraft.common.datacomponents;

import com.arrl.radiocraft.common.init.RadiocraftDataComponent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record EnergyRecord(double energy) {

    public static final Codec<EnergyRecord> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.DOUBLE.fieldOf("energy").forGetter(EnergyRecord::energy)
            ).apply(instance, EnergyRecord::new)
    );
    public static final StreamCodec<ByteBuf, EnergyRecord> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, EnergyRecord::energy,
            EnergyRecord::new
    );
}
