package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Registry class for creative mode tabs
 */
@EventBusSubscriber(modid=Radiocraft.MOD_ID, bus=Bus.MOD)
public class RadiocraftTabs {

	public static CreativeModeTab TAB;

	@SubscribeEvent
	public static void registerCreativeTabs(CreativeModeTabEvent.Register event) {
		// 1.19.3 switched to using event based initialisation for creative mode tabs
		TAB = event.registerCreativeModeTab(Radiocraft.location("main_tab"), builder ->
				builder.icon(() -> new ItemStack(RadiocraftItems.WATERPROOF_WIRE.get()))
						.title(Component.translatable(Radiocraft.translationKey("tabs", "main_tab")))
						.displayItems((featureFlags, output, hasOps) -> {
							output.accept(RadiocraftItems.RADIO_CRYSTAL.get());
							output.accept(RadiocraftItems.RADIO_SPEAKER.get());
							output.accept(RadiocraftItems.MICROPHONE.get());
							output.accept(RadiocraftItems.HAND_MICROPHONE.get());
							output.accept(RadiocraftItems.HF_CIRCUIT_BOARD.get());
							output.accept(RadiocraftItems.SMALL_BATTERY.get());
							output.accept(RadiocraftItems.FERRITE_CORE.get());
							output.accept(RadiocraftItems.COAXIAL_CORE.get());
							output.accept(RadiocraftItems.ANTENNA_ANALYZER.get());
							output.accept(RadiocraftItems.WIRE.get());
							output.accept(RadiocraftItems.WATERPROOF_WIRE.get());
							output.accept(RadiocraftItems.VHF_HANDHELD.get());
							output.accept(RadiocraftItems.SOLAR_PANEL.get());
							output.accept(RadiocraftItems.LARGE_BATTERY.get());
							output.accept(RadiocraftItems.CHARGE_CONTROLLER.get());
							output.accept(RadiocraftItems.SOLAR_WEATHER_STATION.get());
							output.accept(RadiocraftItems.VHF_BASE_STATION.get());
							output.accept(RadiocraftItems.VHF_BASE_STATION.get());
							output.accept(RadiocraftItems.VHF_RECEIVER.get());
							output.accept(RadiocraftItems.VHF_REPEATER.get());
							output.accept(RadiocraftItems.HF_RADIO_10M.get());
							output.accept(RadiocraftItems.HF_RADIO_20M.get());
							output.accept(RadiocraftItems.HF_RADIO_40M.get());
							output.accept(RadiocraftItems.HF_RADIO_80M.get());
							output.accept(RadiocraftItems.HF_RECEIVER.get());
							output.accept(RadiocraftItems.ALL_BAND_RADIO.get());
							output.accept(RadiocraftItems.QRP_RADIO_20M.get());
							output.accept(RadiocraftItems.QRP_RADIO_40M.get());
							output.accept(RadiocraftItems.ANTENNA_POLE.get());
							output.accept(RadiocraftItems.DUPLEXER.get());
							output.accept(RadiocraftItems.ANTENNA_TUNER.get());
							output.accept(RadiocraftItems.ANTENNA_WIRE.get());
							output.accept(RadiocraftItems.ANTENNA_CONNECTOR.get());
							output.accept(RadiocraftItems.COAX_WIRE.get());
							output.accept(RadiocraftItems.DIGITAL_INTERFACE.get());
						})
		);
	}

}
