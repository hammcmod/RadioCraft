package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.commands.CallsignCommands;
import com.arrl.radiocraft.common.commands.SolarWeatherCommands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid=Radiocraft.MOD_ID)
public class RadiocraftCommands {

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(CallsignCommands.BUILDER);
		event.getDispatcher().register(SolarWeatherCommands.BUILDER);
	}

}