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
		event.registerBlock(BE_NETWORKS, (level, pos, state, be, side) -> BENetworksCapability.get(level),
				RadiocraftBlocks.CHARGE_CONTROLLER.get(),
				RadiocraftBlocks.SOLAR_PANEL.get(),
				RadiocraftBlocks.LARGE_BATTERY.get(),
				RadiocraftBlocks.ALL_BAND_RADIO.get(),
				RadiocraftBlocks.HF_RADIO_10M.get(),
				RadiocraftBlocks.HF_RADIO_20M.get(),
				RadiocraftBlocks.HF_RADIO_40M.get(),
				RadiocraftBlocks.HF_RADIO_80M.get(),
				RadiocraftBlocks.HF_RECEIVER.get(),
				RadiocraftBlocks.QRP_RADIO_20M.get(),
				RadiocraftBlocks.QRP_RADIO_40M.get(),
				RadiocraftBlocks.VHF_BASE_STATION.get(),
				RadiocraftBlocks.VHF_RECEIVER.get(),
				RadiocraftBlocks.VHF_REPEATER.get(),
				RadiocraftBlocks.DUPLEXER.get(),
				RadiocraftBlocks.ANTENNA_TUNER.get(),
				RadiocraftBlocks.BALUN_ONE_TO_ONE.get(),
				RadiocraftBlocks.BALUN_TWO_TO_ONE.get(),
				RadiocraftBlocks.J_POLE_ANTENNA.get(),
				RadiocraftBlocks.SLIM_JIM_ANTENNA.get(),
				RadiocraftBlocks.YAGI_ANTENNA.get(),
				RadiocraftBlocks.SATELLITE_DISH.get());
		event.registerBlock(ANTENNA_NETWORKS, (level, pos, state, be, side) -> new AntennaNetworkCapability(), RadiocraftBlocks.HF_RADIO_10M.get());
		event.registerBlock(ANTENNA_NETWORKS, (level, pos, state, be, side) -> new AntennaNetworkCapability(), RadiocraftBlocks.VHF_RECEIVER.get());
        event.registerBlock(ANTENNA_NETWORKS, (level, pos, state, be, side) -> new AntennaNetworkCapability(), RadiocraftBlocks.ALL_BAND_RADIO.get());

        event.registerBlockEntity(BLOCK_ENTITY_CALLSIGNS, RadiocraftBlockEntities.HF_RADIO_ALL_BAND.get(), (entity, ignored) -> BlockEntityCallsignSavedData.get(entity.getLevel().getServer().overworld()));

		event.registerItem(VHF_HANDHELDS, (itemStack, context) -> new VHFHandheldCapability(itemStack), RadiocraftItems.VHF_HANDHELD.get());
		
		// Register SmallBatteryItem with ComponentEnergyStorage and random initial energy (50-70%)
		event.registerItem(Capabilities.EnergyStorage.ITEM, (itemStack, context) -> {
			int capacity = com.arrl.radiocraft.CommonConfig.SMALL_BATTERY_CAPACITY.get();
			// Use custom BATTERY_ENERGY component for energy storage with random initial charge
			return new RandomInitialEnergyStorage(itemStack, com.arrl.radiocraft.common.init.RadiocraftDataComponent.BATTERY_ENERGY.get(), capacity);
		}, RadiocraftItems.SMALL_BATTERY.get());
		
		// Register VHF Handheld with energy storage capability (uses same capacity as small battery)
		event.registerItem(Capabilities.EnergyStorage.ITEM, (itemStack, context) -> {
			int capacity = com.arrl.radiocraft.CommonConfig.SMALL_BATTERY_CAPACITY.get();
			return new net.neoforged.neoforge.energy.ComponentEnergyStorage(
				itemStack,
				com.arrl.radiocraft.common.init.RadiocraftDataComponent.RADIO_ENERGY.get(),
				capacity
			);
		}, RadiocraftItems.VHF_HANDHELD.get());

		// Register block energy capability for Desk Charger (small buffer)
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, RadiocraftBlockEntities.DESK_CHARGER.get(), (be, side) -> {
			// Return the block entity's own energy storage instance so fills/transfers target the BE
			if (be instanceof com.arrl.radiocraft.common.blockentities.DeskChargerBlockEntity desk) {
				return desk.energyStorage;
			}
			// Fallback: new buffer (shouldn't be used)
			return new com.arrl.radiocraft.common.capabilities.BasicEnergyStorage(1000, 250, 250);
		});

		event.registerEntity(ANTENNA_WIRE_HOLDERS, EntityType.PLAYER, (player, context) -> new AntennaWireHolderCapability());
		event.registerEntity(PLAYER_CALLSIGNS, EntityType.PLAYER, (player, context) -> PlayerCallsignSavedData.get(player.getServer().overworld()));
	}
}
