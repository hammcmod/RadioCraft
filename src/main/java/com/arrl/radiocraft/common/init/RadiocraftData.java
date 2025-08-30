package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.solar.SolarEventReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public class RadiocraftData {

	public static final SolarEventReloadListener SOLAR_EVENTS = new SolarEventReloadListener("solar_events");

	@SubscribeEvent
	public static void addReloadListenerEvent(AddReloadListenerEvent event) {
		event.addListener(SOLAR_EVENTS);
	}

}
