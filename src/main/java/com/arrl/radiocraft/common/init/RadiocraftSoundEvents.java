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
	/** The solar weather station printing its report (maintainer's sound, 2026-10-21). */
	public static Supplier<SoundEvent> TELETYPE = SOUND_EVENTS.register("teletype", () -> SoundEvent.createFixedRangeEvent(Radiocraft.id("teletype"), 16.0F));
	/** The bell at the end of the report, and on placing the station. */
	/** The machine's motor running. */
	public static Supplier<SoundEvent> TELETYPE_BACKGROUND = SOUND_EVENTS.register("teletype_background", () -> SoundEvent.createFixedRangeEvent(Radiocraft.id("teletype_background"), 16.0F));
	/** One keystroke of the machine printing; sounds.json picks one of fifteen at random. */
	public static Supplier<SoundEvent> TELETYPE_KEY = SOUND_EVENTS.register("teletype_key", () -> SoundEvent.createFixedRangeEvent(Radiocraft.id("teletype_key"), 16.0F));
	public static Supplier<SoundEvent> TELETYPE_DING = SOUND_EVENTS.register("teletype_ding", () -> SoundEvent.createFixedRangeEvent(Radiocraft.id("teletype_ding"), 16.0F));
	/** The printing sound with a fade at the end; not used yet. */
	public static Supplier<SoundEvent> TELETYPE_FADEOUT = SOUND_EVENTS.register("teletype_fadeout", () -> SoundEvent.createFixedRangeEvent(Radiocraft.id("teletype_fadeout"), 16.0F));

}
