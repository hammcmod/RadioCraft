package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.datacomponents.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RadiocraftDataComponent {

    // The specialized DeferredRegister.DataComponents simplifies data component registration and avoids some generic inference issues with the `DataComponentType.Builder` within a `Supplier`
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Radiocraft.MOD_ID);


    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EnergyRecord>> ENERGY_DATA_COMPONENT = DATA_COMPONENTS.registerComponentType(
            "energy",
            builder -> builder
                    .persistent(EnergyRecord.CODEC)
                    .networkSynchronized(EnergyRecord.STREAM_CODEC)
    );

    // Integer data component for storing energy values (used by batteries)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BATTERY_ENERGY = DATA_COMPONENTS.registerComponentType(
            "battery_energy",
            builder -> builder.persistent(net.minecraft.util.ExtraCodecs.NON_NEGATIVE_INT)
    );

    // Integer data component for storing energy in VHF Handheld radios
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RADIO_ENERGY = DATA_COMPONENTS.registerComponentType(
            "radio_energy",
            builder -> builder.persistent(net.minecraft.util.ExtraCodecs.NON_NEGATIVE_INT)
    );

    /** A radio's hand mic on its cord: which radio, and which lending (see {@link com.arrl.radiocraft.common.items.MicCord}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.arrl.radiocraft.common.items.MicCord>> MIC_CORD = DATA_COMPONENTS.registerComponentType(
            "mic_cord",
            builder -> builder
                    .persistent(com.arrl.radiocraft.common.items.MicCord.CODEC)
                    .networkSynchronized(com.arrl.radiocraft.common.items.MicCord.STREAM_CODEC)
    );

    /** The frequency a handheld direction finder listens on, in Hz. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> DF_FREQUENCY = DATA_COMPONENTS.registerComponentType(
            "df_frequency",
            builder -> builder
                    .persistent(com.mojang.serialization.Codec.FLOAT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.FLOAT)
    );

    /** What is on a tape reel (blank, a server tape, or a recording) and its colourway. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.arrl.radiocraft.common.datacomponents.TapeContents>> TAPE_CONTENTS = DATA_COMPONENTS.registerComponentType(
            "tape_contents",
            builder -> builder
                    .persistent(com.arrl.radiocraft.common.datacomponents.TapeContents.CODEC)
                    .networkSynchronized(com.arrl.radiocraft.common.datacomponents.TapeContents.STREAM_CODEC)
    );

    /** The lines on a roll torn off a teletype. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.arrl.radiocraft.common.datacomponents.TeletypeRollContents>> TELETYPE_ROLL_CONTENTS = DATA_COMPONENTS.registerComponentType(
            "teletype_roll_contents",
            builder -> builder
                    .persistent(com.arrl.radiocraft.common.datacomponents.TeletypeRollContents.CODEC)
                    .networkSynchronized(com.arrl.radiocraft.common.datacomponents.TeletypeRollContents.STREAM_CODEC)
    );

    /** The HF manpack's settings and battery. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.arrl.radiocraft.common.datacomponents.ManpackState>> MANPACK_STATE = DATA_COMPONENTS.registerComponentType(
            "manpack_state",
            builder -> builder
                    .persistent(com.arrl.radiocraft.common.datacomponents.ManpackState.CODEC)
                    .networkSynchronized(com.arrl.radiocraft.common.datacomponents.ManpackState.STREAM_CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HandheldRadioState>> HANDHELD_RADIO_STATE_COMPONENT = DATA_COMPONENTS.registerComponentType(
            "handheld_radio_state",
            builder -> builder
                    .persistent(HandheldRadioState.CODEC)
                    .networkSynchronized(HandheldRadioState.STREAM_CODEC)
    );

}
