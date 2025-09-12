package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.*;
import com.arrl.radiocraft.common.data.BlockEntityCallsignSavedData;
import com.arrl.radiocraft.common.data.PlayerCallsignSavedData;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.*;

@EventBusSubscriber(modid=Radiocraft.MOD_ID)
public class RadiocraftCapabilities {

	public static BlockCapability<IBENetworks, Void> BE_NETWORKS = BlockCapability.createVoid(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "be_networks"), IBENetworks.class);
	public static BlockCapability<IAntennaNetworkCapability, Void> ANTENNA_NETWORKS = BlockCapability.createVoid(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "antenna_networks"), IAntennaNetworkCapability.class);
	public static BlockCapability<IBlockEntityCallsignCapability, Void> BLOCK_ENTITY_CALLSIGNS = BlockCapability.createVoid(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "block_entity_callsigns"), IBlockEntityCallsignCapability.class);

	public static ItemCapability<IVHFHandheldCapability, Void> VHF_HANDHELDS = ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "vhf_handhelds"), IVHFHandheldCapability.class);

	public static EntityCapability<IPlayerCallsignCapability, Void> PLAYER_CALLSIGNS = EntityCapability.createVoid(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "callsigns"), IPlayerCallsignCapability.class);
	public static EntityCapability<IAntennaWireHolderCapability, Void> ANTENNA_WIRE_HOLDERS = EntityCapability.createVoid(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "antenna_wire_holders"), IAntennaWireHolderCapability.class);

	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		//TODO add the rest of the blocks, only adding stuff that is on the short list for testing for now
		event.registerBlock(BE_NETWORKS, (level, pos, state, be, side) -> new BENetworksCapability(), RadiocraftBlocks.CHARGE_CONTROLLER.get());
		event.registerBlock(BE_NETWORKS, (level, pos, state, be, side) -> new BENetworksCapability(), RadiocraftBlocks.VHF_RECEIVER.get());
		event.registerBlock(ANTENNA_NETWORKS, (level, pos, state, be, side) -> new AntennaNetworkCapability(), RadiocraftBlocks.HF_RADIO_10M.get());
		event.registerBlock(ANTENNA_NETWORKS, (level, pos, state, be, side) -> new AntennaNetworkCapability(), RadiocraftBlocks.VHF_RECEIVER.get());
        event.registerBlock(ANTENNA_NETWORKS, (level, pos, state, be, side) -> new AntennaNetworkCapability(), RadiocraftBlocks.ALL_BAND_RADIO.get());

        event.registerBlockEntity(BLOCK_ENTITY_CALLSIGNS, RadiocraftBlockEntities.HF_RADIO_ALL_BAND.get(), (entity, ignored) -> BlockEntityCallsignSavedData.get(entity.getLevel().getServer().overworld()));

		event.registerItem(VHF_HANDHELDS, (itemStack, context) -> VHFHandheldCapability.getCapability(itemStack), RadiocraftItems.VHF_HANDHELD.get());

		event.registerEntity(ANTENNA_WIRE_HOLDERS, EntityType.PLAYER, (player, context) -> new AntennaWireHolderCapability());
		event.registerEntity(PLAYER_CALLSIGNS, EntityType.PLAYER, (player, context) -> PlayerCallsignSavedData.get(player.getServer().overworld()));
	}
}