package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.common.radio.crystal.CrystalQuality;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The poor, good and pure versions of a radio, registered together. The good one keeps the radio's plain id
 * ({@code hf_radio_40m}); the others get the quality on the end ({@code hf_radio_40m_pure}).
 */
public record CrystalVariants<R, T extends R>(DeferredHolder<R, T> poor, DeferredHolder<R, T> good, DeferredHolder<R, T> pure) {

    public static <R, T extends R> CrystalVariants<R, T> register(DeferredRegister<R> register, String name, Function<CrystalQuality, ? extends T> factory) {
        return new CrystalVariants<>(
                register.register(name + CrystalQuality.POOR.radioSuffix(), () -> factory.apply(CrystalQuality.POOR)),
                register.register(name + CrystalQuality.GOOD.radioSuffix(), () -> factory.apply(CrystalQuality.GOOD)),
                register.register(name + CrystalQuality.PURE.radioSuffix(), () -> factory.apply(CrystalQuality.PURE)));
    }

    public DeferredHolder<R, T> get(CrystalQuality quality) {
        return switch(quality) {
            case POOR -> poor;
            case GOOD -> good;
            case PURE -> pure;
        };
    }

    /** Pure first, then good, then poor: the order they show up in the creative tab. */
    public List<DeferredHolder<R, T>> all() {
        return List.of(pure, good, poor);
    }

    public Stream<T> values() {
        return all().stream().map(DeferredHolder::get);
    }
}
