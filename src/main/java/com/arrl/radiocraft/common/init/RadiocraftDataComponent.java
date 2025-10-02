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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HandheldRadioState>> HANDHELD_RADIO_STATE_COMPONENT = DATA_COMPONENTS.registerComponentType(
            "handheld_radio_state",
            builder -> builder
                    .persistent(HandheldRadioState.CODEC)
                    .networkSynchronized(HandheldRadioState.STREAM_CODEC)
    );

}
