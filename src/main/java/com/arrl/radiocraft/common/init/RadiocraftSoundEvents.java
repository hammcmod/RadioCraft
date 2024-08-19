package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RadiocraftSoundEvents {

	public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Radiocraft.MOD_ID);

	public static Supplier<SoundEvent> STATIC = SOUND_EVENTS.register("static", () -> SoundEvent.createFixedRangeEvent(Radiocraft.id("static"), 32.0F));
	public static Supplier<SoundEvent> MORSE = SOUND_EVENTS.register("morse", () -> SoundEvent.createFixedRangeEvent(Radiocraft.id("morse"), 32.0F));

}
