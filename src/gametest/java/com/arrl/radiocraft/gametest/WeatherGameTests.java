package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.weather.StormReport;
import com.arrl.radiocraft.api.weather.WeatherReport;
import com.arrl.radiocraft.api.weather.WeatherSource;
import com.arrl.radiocraft.api.weather.WeatherSources;
import com.arrl.radiocraft.common.radio.wefax.WefaxChart;
import com.arrl.radiocraft.common.radio.wefax.WefaxPrints;
import com.arrl.radiocraft.compat.anotherweathermod.AwmCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Weather sources, WX reports and WEFAX charts. The chart tests use a made-up source with one storm, so they do not
 * depend on the weather mod; the Another Weather Mod test runs when that mod is loaded (it is in dev runs).
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WeatherGameTests {

	/** One thunderstorm of radius 160 sitting 320 blocks east of the station, raining 1 inside. */
	private static WeatherSource oneStorm(BlockPos station) {
		double sx = station.getX() + 320, sz = station.getZ();
		return new WeatherSource() {
			@Override public String name() { return "TEST"; }
			@Override public boolean handles(Level level) { return false; }
			@Override public boolean isLocal() { return true; }
			@Override public WeatherReport reportAt(ServerLevel level, BlockPos pos, long gameTime) {
				boolean in = Math.hypot(pos.getX() - sx, pos.getZ() - sz) < 160;
				return new WeatherReport(in ? WeatherReport.Precipitation.RAIN : WeatherReport.Precipitation.NONE, in ? 1 : 0, 0,
						in ? 15 : 4, 270, 20, in ? 1000 : 1013, 60, Set.of());
			}
			@Override public double rainAt(ServerLevel level, double x, double z, long gameTime) {
				return Math.hypot(x - sx, z - sz) < 160 ? 1 : 0;
			}
			@Override public double thunderAt(ServerLevel level, double x, double z, long gameTime) { return 0; }
			@Override public List<StormReport> storms(ServerLevel level, BlockPos center, double radius, boolean forecast) {
				long now = level.getGameTime();
				return List.of(new StormReport(7, StormReport.Kind.THUNDERSTORM, false, now - 100,
						List.of(new StormReport.TrackPoint(now, sx, sz, 160, 1), new StormReport.TrackPoint(now + 72000, sx - 600, sz, 160, 1))));
			}
			@Override public Optional<Arrival> nextArrival(ServerLevel level, BlockPos pos, long horizon) { return Optional.empty(); }
			@Override public long forecastHorizon(ServerLevel level) { return 24000; }
		};
	}

	/** The radar chart is ink where it rains and paper where it does not, with a header. */
	@GameTest(template = GameTestUtil.EMPTY)
	public static void radarChartShowsTheStorm(GameTestHelper helper) {
		BlockPos station = helper.absolutePos(new BlockPos(8, 2, 8));
		byte[] chart = WefaxChart.paint(helper.getLevel(), oneStorm(station), station, WefaxChart.Type.RADAR, "W1AW");
		WefaxChart.Frame frame = WefaxChart.frame(station, List.of());
		// 320 blocks east at 16 blocks a pixel is 20 pixels right of the station; sample the storm's solid core,
		// clear of its symbol, and the clear sky to the west.
		int cx = frame.pixelX(station.getX() + 320), cz = frame.pixelZ(station.getZ());
		int inked = 0, clear = 0;
		for(int dz = 4; dz <= 7; dz++)
			for(int dx = -2; dx <= 2; dx++) {
				if(chart[(cx + dx) + (cz + dz) * WefaxChart.SIZE] != WefaxChart.PAPER)
					inked++;
				if(chart[(frame.pixelX(station.getX() - 400) + dx) + (cz + dz) * WefaxChart.SIZE] == WefaxChart.PAPER)
					clear++;
			}
		helper.assertTrue(inked == 20, "the storm's core is solid ink, got " + inked + "/20");
		helper.assertTrue(clear >= 15, "the sky west of the station is mostly paper, got " + clear + "/20");
		int headerInk = 0;
		for(int x = 0; x < WefaxChart.SIZE; x++)
			if(chart[x + 4 * WefaxChart.SIZE] != WefaxChart.PAPER)
				headerInk++;
		helper.assertTrue(headerInk > 10, "the header has its title");
		helper.succeed();
	}

	/**
	 * Saves a chart as a PNG in the GameTest run folder ({@code runs/gametest/wefax}), to look at what prints look like.
	 * Nothing checks it; a failure to write is ignored.
	 */
	static void dump(byte[] chart, String name) {
		try {
			var image = new java.awt.image.BufferedImage(WefaxChart.SIZE * 4, WefaxChart.SIZE * 4, java.awt.image.BufferedImage.TYPE_INT_RGB);
			for(int z = 0; z < WefaxChart.SIZE; z++)
				for(int x = 0; x < WefaxChart.SIZE; x++) {
					int abgr = net.minecraft.world.level.material.MapColor.getColorFromPackedId(chart[x + z * WefaxChart.SIZE]);
					int rgb = (abgr & 0xFF) << 16 | (abgr >> 8 & 0xFF) << 8 | (abgr >> 16 & 0xFF);
					for(int d = 0; d < 16; d++)
						image.setRGB(x * 4 + d % 4, z * 4 + d / 4, rgb);
				}
			java.nio.file.Path dir = java.nio.file.Path.of("wefax");
			java.nio.file.Files.createDirectories(dir);
			javax.imageio.ImageIO.write(image, "png", dir.resolve(name + ".png").toFile());
		}
		catch(java.io.IOException | RuntimeException ignored) {
		}
	}

	/** A print is a locked, filled map with the chart on it and the text report in its tooltip. */
	@GameTest(template = GameTestUtil.EMPTY)
	public static void printsAreMaps(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos station = helper.absolutePos(new BlockPos(8, 2, 8));
		for(WefaxChart.Type type : WefaxChart.Type.values()) {
			ItemStack print = WefaxPrints.print(level, oneStorm(station), station, type, "W1AW");
			helper.assertTrue(print.is(Items.FILLED_MAP), "a filled map");
			MapItemSavedData data = level.getMapData(print.get(DataComponents.MAP_ID));
			helper.assertTrue(data != null && data.locked, "locked map data");
			var lore = print.get(DataComponents.LORE);
			helper.assertTrue(lore != null && lore.lines().getFirst().getString().startsWith("WX W1AW"), "the report heads the tooltip");
		}
		helper.succeed();
	}

	/**
	 * Hurricane winds blow an antenna pole down sooner or later (a 30 % chance each time the wind picks it at 60 m/s),
	 * and never with {@code wind_antenna_damage_enabled} off.
	 */
	@GameTest(template = GameTestUtil.EMPTY)
	public static void windBlowsAntennasDown(GameTestHelper helper) {
		if(!AwmCompat.isLoaded()) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos pole = new BlockPos(8, 2, 8);
		helper.setBlock(pole, com.arrl.radiocraft.common.init.RadiocraftBlocks.ANTENNA_POLE.get());
		com.arrl.radiocraft.RadiocraftServerConfig.WIND_ANTENNA_DAMAGE_ENABLED.set(false);
		try {
			for(int i = 0; i < 40; i++)
				com.itcmd.anotherweathermod.world.WindDamage.apply(level, helper.absolutePos(pole), 60, 0, 60);
			helper.assertBlockPresent(com.arrl.radiocraft.common.init.RadiocraftBlocks.ANTENNA_POLE.get(), pole);
		}
		finally {
			com.arrl.radiocraft.RadiocraftServerConfig.WIND_ANTENNA_DAMAGE_ENABLED.set(true);
		}
		for(int i = 0; i < 60 && !helper.getBlockState(pole).isAir(); i++)
			com.itcmd.anotherweathermod.world.WindDamage.apply(level, helper.absolutePos(pole), 60, 0, 60);
		helper.assertBlockNotPresent(com.arrl.radiocraft.common.init.RadiocraftBlocks.ANTENNA_POLE.get(), pole);
		helper.succeed();
	}

	/** With Another Weather Mod, the WX report sees its storms. Storms are level-wide: a batch of its own. */
	@GameTest(template = GameTestUtil.EMPTY, batch = "awm_weather")
	public static void anotherWeatherModStorms(GameTestHelper helper) {
		if(!AwmCompat.isLoaded()) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(8, 2, 8));
		WeatherSource source = WeatherSources.get(level);
		helper.assertTrue(source.name().equals("AWM"), "the level's weather comes from Another Weather Mod, got " + source.name());
		var commands = level.getServer().getCommands();
		var at = level.getServer().createCommandSourceStack().withLevel(level).withPosition(Vec3.atCenterOf(pos)).withSuppressedOutput();
		commands.performPrefixedCommand(at, "awm clear");
		commands.performPrefixedCommand(at, "awm spawn thunderstorm 0.9 96");
		helper.assertTrue(source.rainAt(level, pos.getX(), pos.getZ(), level.getGameTime()) > 0.5, "it rains under the storm");
		helper.assertTrue(WeatherSources.isRaining(level, pos), "isRaining agrees");
		helper.assertTrue(source.storms(level, pos, 100, false).size() == 1, "one storm reported");

		// A busier sky for the charts: a supercell to the east, a shower on its way, then print each chart.
		var east = at.withPosition(Vec3.atCenterOf(pos.east(420)));
		commands.performPrefixedCommand(east, "awm spawn supercell 1 180");
		commands.performPrefixedCommand(at.withPosition(Vec3.atCenterOf(pos.north(300))), "awm approach shower 20");
		for(WefaxChart.Type type : WefaxChart.Type.values())
			dump(WefaxChart.paint(level, source, pos, type, "W1AW"), type.name().toLowerCase(java.util.Locale.ROOT));
		commands.performPrefixedCommand(at, "awm clear");
		helper.assertFalse(WeatherSources.isRaining(level, pos), "storm gone");
		helper.succeed();
	}
}
