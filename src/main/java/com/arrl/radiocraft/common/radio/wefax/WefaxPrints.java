package com.arrl.radiocraft.common.radio.wefax;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.weather.WeatherSource;
import com.arrl.radiocraft.common.radio.weather.WeatherReports;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * A WEFAX chart as something to hold: a locked vanilla map with the chart painted on it, so it can be held, put in an
 * item frame, copied at a cartography table or kept in a chest, with the station's text report in its tooltip. A
 * print does not change after it is made; a new transmission makes a new print.
 */
public final class WefaxPrints {

	/** The tint of a fax print in the inventory: off-white paper. */
	static final int PAPER_TINT = 0xE8E4D8;

	private WefaxPrints() {
	}

	/**
	 * Prints {@code type} for the area around {@code center}, from {@code source}.
	 *
	 * @param station the sending station's callsign, or empty
	 */
	public static ItemStack print(ServerLevel level, WeatherSource source, BlockPos center, WefaxChart.Type type, String station) {
		byte[] pixels = WefaxChart.paint(level, source, center, type, station);
		MapId id = level.getFreeMapId();
		MapItemSavedData data = MapItemSavedData.createFresh(center.getX(), center.getZ(), (byte)0, false, false, level.dimension()).locked();
		for(int z = 0; z < WefaxChart.SIZE; z++)
			for(int x = 0; x < WefaxChart.SIZE; x++)
				data.updateColor(x, z, pixels[x + z * WefaxChart.SIZE]);
		level.setMapData(id, data);

		ItemStack stack = new ItemStack(Items.FILLED_MAP);
		stack.set(DataComponents.MAP_ID, id);
		stack.set(DataComponents.ITEM_NAME, Component.translatableWithFallback(
				Radiocraft.translationKey("item", "wefax_chart." + type.name().toLowerCase(java.util.Locale.ROOT)),
				switch(type) {
					case RADAR -> "WEFAX Radar Chart";
					case FORECAST -> "WEFAX Forecast Chart";
					case SURFACE -> "WEFAX Surface Analysis";
				}));
		stack.set(DataComponents.MAP_COLOR, new MapItemColor(PAPER_TINT));
		List<Component> lore = new ArrayList<>();
		for(String line : WeatherReports.lines(level, source, center, station))
			lore.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
		stack.set(DataComponents.LORE, new ItemLore(lore));
		return stack;
	}
}
