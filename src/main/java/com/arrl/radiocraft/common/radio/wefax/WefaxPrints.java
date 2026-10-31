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
		return printChart(level, source, center, type, station).stack();
	}

	/** A printed sheet, and its picture (for the receiver's screen to draw as it prints). */
	public record Print(ItemStack stack, byte[] pixels) {}

	/** {@link #print}, with the picture. */
	public static Print printChart(ServerLevel level, WeatherSource source, BlockPos center, WefaxChart.Type type, String station) {
		byte[] pixels = WefaxChart.paint(level, source, center, type, station);
		List<Component> lore = new ArrayList<>();
		for(String line : WeatherReports.lines(level, source, center, station))
			lore.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
		Component name = Component.translatableWithFallback(
				Radiocraft.translationKey("item", "wefax_chart." + type.name().toLowerCase(java.util.Locale.ROOT)),
				switch(type) {
					case RADAR -> "WEFAX Radar Chart";
					case FORECAST -> "WEFAX Forecast Chart";
					case SURFACE -> "WEFAX Surface Analysis";
				});
		return new Print(sheet(level, center, pixels, name, lore), pixels);
	}

	/** A sheet of text, {@code lines} under {@code title}: the solar weather report. */
	public static Print printText(ServerLevel level, BlockPos center, Component name, String title, List<String> lines) {
		return printText(level, center, name, title, lines, false);
	}

	/** As {@link #printText(ServerLevel, BlockPos, Component, String, List)}, with the WEFAX banner bottom right (the solar sheets). */
	public static Print printText(ServerLevel level, BlockPos center, Component name, String title, List<String> lines, boolean banner) {
		byte[] pixels = WefaxChart.paintText(title, lines, banner);
		List<Component> lore = new ArrayList<>();
		for(String line : lines)
			lore.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
		return new Print(sheet(level, center, pixels, name, lore), pixels);
	}

	/** The title a solar bulletin from {@code from} prints under. */
	public static String bulletinTitle(String from) {
		return "SOLAR WEATHER " + from;
	}

	/** A solar bulletin's lines as printed: the report, then who sent it. */
	public static List<String> bulletinSheet(String from, List<String> lines) {
		List<String> sheet = new ArrayList<>(lines);
		sheet.add("FROM " + from);
		return sheet;
	}

	/** The picture of a solar bulletin: what the sending radio's fax tones carry and the receiver prints. */
	public static byte[] bulletinPixels(String from, List<String> lines) {
		return WefaxChart.paintText(bulletinTitle(from), bulletinSheet(from, lines), true);
	}

	/** Paints {@code pixels} over the picture of a sheet already printed (lines lost in the noise, say). */
	public static void repaint(ServerLevel level, ItemStack sheet, byte[] pixels) {
		MapId id = sheet.get(DataComponents.MAP_ID);
		MapItemSavedData data = id == null ? null : level.getMapData(id);
		if(data == null || pixels.length != WefaxChart.SIZE * WefaxChart.SIZE)
			return;
		for(int z = 0; z < WefaxChart.SIZE; z++)
			for(int x = 0; x < WefaxChart.SIZE; x++)
				data.updateColor(x, z, pixels[x + z * WefaxChart.SIZE]);
		data.setDirty();
	}

	/** A locked map of {@code pixels}, named and with {@code lore}, on fax paper. */
	private static ItemStack sheet(ServerLevel level, BlockPos center, byte[] pixels, Component name, List<Component> lore) {
		MapId id = level.getFreeMapId();
		MapItemSavedData data = MapItemSavedData.createFresh(center.getX(), center.getZ(), (byte)0, false, false, level.dimension()).locked();
		for(int z = 0; z < WefaxChart.SIZE; z++)
			for(int x = 0; x < WefaxChart.SIZE; x++)
				data.updateColor(x, z, pixels[x + z * WefaxChart.SIZE]);
		level.setMapData(id, data);
		ItemStack stack = new ItemStack(Items.FILLED_MAP);
		stack.set(DataComponents.MAP_ID, id);
		stack.set(DataComponents.ITEM_NAME, name);
		stack.set(DataComponents.MAP_COLOR, new MapItemColor(PAPER_TINT));
		stack.set(DataComponents.LORE, new ItemLore(lore));
		return stack;
	}
}
