package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.BandReloadListener;
import com.arrl.radiocraft.common.radio.solar.SolarEventReloadListener;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod.EventBusSubscriber(modid=Radiocraft.MOD_ID, bus=Bus.FORGE)
public class RadiocraftData {

	public static final SolarEventReloadListener SOLAR_EVENTS = new SolarEventReloadListener("solar_events");
	public static final BandReloadListener BANDS = new BandReloadListener("bands");

	@SubscribeEvent
	public static void addReloadListenerEvent(AddReloadListenerEvent event) {
		event.addListener(SOLAR_EVENTS);
		event.addListener(BANDS);
	}

}
