package com.arrl.radiocraft.common;

import com.arrl.radiocraft.Radiocraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DataComponent {

    public record EnergyRecord(double energy) {

    }

    // The specialized DeferredRegister.DataComponents simplifies data component registration and avoids some generic inference issues with the `DataComponentType.Builder` within a `Supplier`
    public static final DeferredRegister.DataComponents REGISTRAR = DeferredRegister.createDataComponents(Radiocraft.MOD_ID);

    // Basic codec
    public static final Codec<EnergyRecord> BASIC_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.DOUBLE.fieldOf("energy").forGetter(EnergyRecord::energy)
            ).apply(instance, EnergyRecord::new)
    );
    public static final StreamCodec<ByteBuf, EnergyRecord> BASIC_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, EnergyRecord::energy,
            EnergyRecord::new
    );

    // Unit stream codec if nothing should be sent across the network
    public static final StreamCodec<ByteBuf, EnergyRecord> UNIT_STREAM_CODEC = StreamCodec.unit(new EnergyRecord(0));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EnergyRecord>> ENERGY_DATA_COMPONENT = REGISTRAR.registerComponentType(
            "basic",
            builder -> builder
                    // The codec to read/write the data to disk
                    .persistent(BASIC_CODEC)
                    // The codec to read/write the data across the network
                    .networkSynchronized(BASIC_STREAM_CODEC)
    );

}
