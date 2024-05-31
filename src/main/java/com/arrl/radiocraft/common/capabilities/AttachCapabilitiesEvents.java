package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid=Radiocraft.MOD_ID, bus=Bus.FORGE)
public class AttachCapabilitiesEvents {

	@SubscribeEvent
	public static void onAttachCapabilitiesLevel(AttachCapabilitiesEvent<Level> event) {
		Level level = event.getObject();

		if(!level.isClientSide) {
			if(level.dimension() == Level.OVERWORLD)
				event.addCapability(Radiocraft.id("callsigns"), new CallsignCapabilityProvider());

			event.addCapability(Radiocraft.id("antenna_networks"), new AntennaNetworkCapabilityProvider());
			event.addCapability(Radiocraft.id("be_networks"), new BENetworksCapabilityProvider(level));
		}
	}

	@SubscribeEvent
	public static void onAttachCapabilitiesEntity(AttachCapabilitiesEvent<Entity> event) {
		Entity entity = event.getObject();

		if(entity instanceof Player)
			event.addCapability(Radiocraft.id("wire_holder"), new AntennaWireHolderCapabilityProvider());
	}

	@SubscribeEvent
	public static void onAttachCapabilitiesItemStack(AttachCapabilitiesEvent<ItemStack> event) {
		ItemStack stack = event.getObject();

		if(stack.getItem() == RadiocraftItems.VHF_HANDHELD.get())
			event.addCapability(Radiocraft.id("vhf_handheld"), new VHFHandheldCapabilityProvider());
	}

}
