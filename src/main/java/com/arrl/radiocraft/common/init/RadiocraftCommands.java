package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.commands.CallsignCommands;
import com.arrl.radiocraft.common.commands.SolarWeatherCommands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid=Radiocraft.MOD_ID)
public class RadiocraftCommands {

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(CallsignCommands.BUILDER);
		event.getDispatcher().register(SolarWeatherCommands.BUILDER);
	}
}