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
						})
		);
	}

}
