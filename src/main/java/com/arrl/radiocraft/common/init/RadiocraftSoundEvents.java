package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RadiocraftSoundEvents {

	public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Radiocraft.MOD_ID);

	public static RegistryObject<SoundEvent> STATIC = SOUND_EVENTS.register("static", () -> SoundEvent.createFixedRangeEvent(Radiocraft.id("static"), 32.0F));
	public static RegistryObject<SoundEvent> MORSE = SOUND_EVENTS.register("morse", () -> SoundEvent.createFixedRangeEvent(Radiocraft.id("morse"), 32.0F));

}
