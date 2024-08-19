package com.arrl.radiocraft.client.events;


import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.entity.AntennaWireEntityRenderer;
import com.arrl.radiocraft.client.screens.*;
import com.arrl.radiocraft.client.screens.radios.*;
import com.arrl.radiocraft.common.init.RadiocraftEntityTypes;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid= Radiocraft.MOD_ID, bus= EventBusSubscriber.Bus.MOD, value= Dist.CLIENT)
public class ClientSetupEvents {

	@SubscribeEvent
	public static void registerScreens(RegisterMenuScreensEvent event) {
		event.register(RadiocraftMenuTypes.LARGE_BATTERY.get(), LargeBatteryScreen::new);
		event.register(RadiocraftMenuTypes.SOLAR_PANEL.get(), SolarPanelScreen::new);
		event.register(RadiocraftMenuTypes.CHARGE_CONTROLLER.get(), ChargeControllerScreen::new);
		event.register(RadiocraftMenuTypes.HF_RADIO_10M.get(), HFRadio10mScreen::new);
		event.register(RadiocraftMenuTypes.HF_RADIO_20M.get(), HFRadio20mScreen::new);
		event.register(RadiocraftMenuTypes.HF_RADIO_40M.get(), HFRadio40mScreen::new);
		event.register(RadiocraftMenuTypes.HF_RADIO_80M.get(), HFRadio80mScreen::new);
		event.register(RadiocraftMenuTypes.QRP_RADIO_20M.get(), QRPRadio20mScreen::new);
		event.register(RadiocraftMenuTypes.QRP_RADIO_40M.get(), QRPRadio40mScreen::new);
		event.register(RadiocraftMenuTypes.VHF_BASE_STATION.get(), VHFBaseStationScreen::new);
		event.register(RadiocraftMenuTypes.VHF_RECEIVER.get(), VHFReceiverScreen::new);
		event.register(RadiocraftMenuTypes.HF_RECEIVER.get(), HFReceiverScreen::new);
	}
	
	@SubscribeEvent
	public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(RadiocraftEntityTypes.ANTENNA_WIRE.get(), AntennaWireEntityRenderer::new);
	}
}
