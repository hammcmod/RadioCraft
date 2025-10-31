package com.arrl.radiocraft.client.events;


import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.entity.AntennaWireEntityRenderer;
import com.arrl.radiocraft.client.screens.*;
import com.arrl.radiocraft.client.screens.radios.*;
import com.arrl.radiocraft.common.init.RadiocraftEntityTypes;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.blockentities.DeskChargerBlockEntity;
import com.arrl.radiocraft.client.render.DeskChargerBlockRenderer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid= Radiocraft.MOD_ID, value= Dist.CLIENT)
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
		event.register(RadiocraftMenuTypes.HF_RADIO_ALL_BAND.get(), HFRadioAllBandScreen::new);
		event.register(RadiocraftMenuTypes.QRP_RADIO_20M.get(), QRPRadio20mScreen::new);
		event.register(RadiocraftMenuTypes.QRP_RADIO_40M.get(), QRPRadio40mScreen::new);
		event.register(RadiocraftMenuTypes.VHF_BASE_STATION.get(), VHFBaseStationScreen::new);
		event.register(RadiocraftMenuTypes.VHF_RECEIVER.get(), VHFReceiverScreen::new);
		event.register(RadiocraftMenuTypes.HF_RECEIVER.get(), HFReceiverScreen::new);
		event.register(RadiocraftMenuTypes.DESK_CHARGER.get(), DeskChargerScreen::new);
		event.register(RadiocraftMenuTypes.DIGITAL_INTERFACE.get(), DigitalInterfaceScreen::new);
		event.register(RadiocraftMenuTypes.DUPLEXER.get(), DuplexerScreen::new);
		event.register(RadiocraftMenuTypes.ANTENNA_TUNER.get(), AntennaTunerScreen::new);
		event.register(RadiocraftMenuTypes.VHF_REPEATER.get(), VHFRepeaterScreen::new);
	}
	
	@SubscribeEvent
	public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(RadiocraftEntityTypes.ANTENNA_WIRE.get(), AntennaWireEntityRenderer::new);
		// Desk charger geo renderer
		// Cast to the concrete BlockEntityType generic to satisfy method signature
		// Use a lambda with an unchecked raw cast to avoid generic signature issues in the registration helper.
		event.registerBlockEntityRenderer((net.minecraft.world.level.block.entity.BlockEntityType) RadiocraftBlockEntities.DESK_CHARGER.get(), ctx -> new DeskChargerBlockRenderer(ctx));
		// Satellite dish geo renderer
		event.registerBlockEntityRenderer((net.minecraft.world.level.block.entity.BlockEntityType) RadiocraftBlockEntities.SATELLITE_DISH.get(), ctx -> new com.arrl.radiocraft.client.render.SatelliteDishRenderer(ctx));
	}
}
