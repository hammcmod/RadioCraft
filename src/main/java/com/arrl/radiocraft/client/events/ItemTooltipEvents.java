package com.arrl.radiocraft.client.events;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.SearchAliases;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Gives every Radiocraft item its {@code tooltip.radiocraft.<id>} line, so plain items and blocks whose class adds no
 * tooltip still say what they are for. Items that already add that line themselves are left alone. For the creative
 * search's index (built with no player) it also adds the numbers in the name as words ({@link SearchAliases}).
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID, value = Dist.CLIENT)
public class ItemTooltipEvents {

	@SubscribeEvent
	public static void onItemTooltip(ItemTooltipEvent event) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
		if(!Radiocraft.MOD_ID.equals(id.getNamespace()))
			return;
		if(event.getEntity() == null) // The creative search indexes tooltips with no player: give it the numbers as words too.
			for(String alias : SearchAliases.of(event.getItemStack().getHoverName().getString()))
				event.getToolTip().add(Component.literal(alias));
		String key = Radiocraft.translationKey("tooltip", id.getPath());
		if(!I18n.exists(key))
			return;
		for(Component line : event.getToolTip())
			if(line.getContents() instanceof TranslatableContents t && t.getKey().equals(key))
				return;
		event.getToolTip().add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
	}
}
