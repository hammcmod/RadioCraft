package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.*;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.*;

@EventBusSubscriber(modid=Radiocraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class RadiocraftCapabilities {

	public static BlockCapability<ICallsignCapability, Void> CALLSIGNS = BlockCapability.createVoid(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "callsigns"), ICallsignCapability.class);
	public static BlockCapability<IBENetworks, Void> BE_NETWORKS = BlockCapability.createVoid(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "be_networks"), IBENetworks.class);
	public static BlockCapability<IAntennaNetworkCapability, Void> ANTENNA_NETWORKS = BlockCapability.createVoid(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "antenna_networks"), IAntennaNetworkCapability.class);
	public static ItemCapability<IVHFHandheldCapability, Void> VHF_HANDHELDS = ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "vhf_handhelds"), IVHFHandheldCapability.class);
	public static EntityCapability<IAntennaWireHolderCapability, Void> ANTENNA_WIRE_HOLDERS = EntityCapability.createVoid(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "antenna_wire_holders"), IAntennaWireHolderCapability.class);

	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		//TODO add the rest of the blocks, only adding stuff that is on the short list for testing for now
		event.registerBlock(BE_NETWORKS, (level, pos, state, be, side) -> new BENetworksCapability(), RadiocraftBlocks.CHARGE_CONTROLLER.get());
		event.registerBlock(BE_NETWORKS, (level, pos, state, be, side) -> new BENetworksCapability(), RadiocraftBlocks.VHF_RECEIVER.get());
		event.registerBlock(CALLSIGNS, (level, pos, state, be, side) -> new CallsignCapability(), RadiocraftBlocks.HF_RADIO_10M.get());
		event.registerBlock(ANTENNA_NETWORKS, (level, pos, state, be, side) -> new AntennaNetworkCapability(), RadiocraftBlocks.HF_RADIO_10M.get());
		event.registerBlock(ANTENNA_NETWORKS, (level, pos, state, be, side) -> new AntennaNetworkCapability(), RadiocraftBlocks.VHF_RECEIVER.get());

		event.registerItem(VHF_HANDHELDS, (itemStack, context) -> new VHFHandheldCapability(itemStack), RadiocraftItems.VHF_HANDHELD.get());
		event.registerItem(Capabilities.EnergyStorage.ITEM, (itemStack, context) -> new BatteryCapability(itemStack), RadiocraftItems.SMALL_BATTERY.get());

		event.registerEntity(ANTENNA_WIRE_HOLDERS, EntityType.PLAYER, (myEntity, context) -> new AntennaWireHolderCapability());
	}
}
