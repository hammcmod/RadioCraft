package com.arrl.radiocraft.client.events;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.render.AntennaWireEntityRenderer;
import com.arrl.radiocraft.client.screens.*;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import com.arrl.radiocraft.entity.AntennaWireEntity;
import com.arrl.radiocraft.entity.RadiocraftEntityTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid=Radiocraft.MOD_ID, bus=Bus.MOD, value=Dist.CLIENT)
public class ClientSetupEvents {

	@SubscribeEvent
	public static void clientSetup(FMLClientSetupEvent event) {
		MenuScreens.register(RadiocraftMenuTypes.LARGE_BATTERY.get(), LargeBatteryScreen::new);
		MenuScreens.register(RadiocraftMenuTypes.SOLAR_PANEL.get(), SolarPanelScreen::new);
		MenuScreens.register(RadiocraftMenuTypes.CHARGE_CONTROLLER.get(), ChargeControllerScreen::new);
		MenuScreens.register(RadiocraftMenuTypes.HF_RADIO_10M.get(), HfRadio10mScreen::new);
	}

	@SubscribeEvent
	public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(RadiocraftEntityTypes.ANTENNA_WIRE.get(), AntennaWireEntityRenderer::new);
	}

}
