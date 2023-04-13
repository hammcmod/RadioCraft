package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import javax.annotation.Nonnull;

@Mod.EventBusSubscriber(modid=Radiocraft.MOD_ID, bus=Bus.FORGE)
public class AttachCapabilitiesEvents {

	@SubscribeEvent
	public static void onAttachCapabilities(@Nonnull final AttachCapabilitiesEvent<Level> event) {
		Level level = event.getObject();

		if(!level.isClientSide && level.dimension() == Level.OVERWORLD) {
			event.addCapability(Radiocraft.location("callsigns"), new CallsignCapabilityProvider());
		}
	}

}
