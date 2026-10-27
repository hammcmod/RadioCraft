package com.arrl.radiocraft.common.events;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/** Radio things villagers sell. */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public class VillagerTradeEvents {

	/** Journeyman cartographers sell the world map module: twelve emeralds and a compass. */
	@SubscribeEvent
	public static void onVillagerTrades(VillagerTradesEvent event) {
		if(event.getType() != VillagerProfession.CARTOGRAPHER)
			return;
		event.getTrades().get(3).add((trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, 12),
				java.util.Optional.of(new ItemCost(Items.COMPASS, 1)), new ItemStack(RadiocraftItems.WORLD_MAP_MODULE.get()), 4, 15, 0.2F));
	}
}
