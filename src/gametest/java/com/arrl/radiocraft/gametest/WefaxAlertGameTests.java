package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.weather.StormReport;
import com.arrl.radiocraft.api.weather.WeatherReport;
import com.arrl.radiocraft.api.weather.WeatherSource;
import com.arrl.radiocraft.common.blockentities.WefaxReceiverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

/**
 * The WEFAX redstone levels (maintainer, 2026-10-30): 1 for a message, 2 for a severe thunderstorm, 3 for a tornado
 * watch, 4 for a hurricane, the highest when several come at once. Checked against a made-up weather source.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WefaxAlertGameTests {

	/** Calm weather with the given storms about, and a tornado watch or not. */
	private record Fixed(List<StormReport.Kind> kinds, boolean watch) implements WeatherSource {
		@Override public String name() { return "test"; }
		@Override public boolean handles(Level level) { return true; }
		@Override public boolean isLocal() { return true; }
		@Override public WeatherReport reportAt(ServerLevel level, BlockPos pos, long gameTime) { return WeatherReport.CALM; }
		@Override public double rainAt(ServerLevel level, double x, double z, long gameTime) { return 0.0D; }
		@Override public double thunderAt(ServerLevel level, double x, double z, long gameTime) { return 0.0D; }
		@Override public Optional<Arrival> nextArrival(ServerLevel level, BlockPos pos, long horizon) { return Optional.empty(); }
		@Override public long forecastHorizon(ServerLevel level) { return 0L; }
		@Override public boolean tornadoWatch(ServerLevel level, BlockPos pos) { return watch; }

		@Override
		public List<StormReport> storms(ServerLevel level, BlockPos center, double radius, boolean forecast) {
			List<StormReport.TrackPoint> track = List.of(new StormReport.TrackPoint(0L, center.getX() + 100, center.getZ(), 200.0D, 1.0F));
			return kinds.stream().map(kind -> new StormReport(kinds.indexOf(kind), kind, false, 0L, track, 1)).toList();
		}
	}

	private static int level(GameTestHelper helper, List<StormReport.Kind> kinds, boolean watch) {
		return WefaxReceiverBlockEntity.alertLevel(helper.getLevel(), new Fixed(kinds, watch), helper.absolutePos(BlockPos.ZERO));
	}

	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20)
	public static void alertLevelsFollowTheWorstWeather(GameTestHelper helper) {
		helper.assertTrue(level(helper, List.of(), false) == 1, "Calm weather is not level 1");
		helper.assertTrue(level(helper, List.of(StormReport.Kind.THUNDERSTORM), false) == 1, "A plain thunderstorm is not level 1");
		helper.assertTrue(level(helper, List.of(StormReport.Kind.SEVERE_THUNDERSTORM), false) == 2, "A severe thunderstorm is not level 2");
		helper.assertTrue(level(helper, List.of(StormReport.Kind.SHOWER), true) == 3, "A tornado watch is not level 3");
		helper.assertTrue(level(helper, List.of(StormReport.Kind.SEVERE_THUNDERSTORM, StormReport.Kind.TORNADO), false) == 3,
				"A tornado with a severe thunderstorm is not level 3");
		helper.assertTrue(level(helper, List.of(StormReport.Kind.TORNADO, StormReport.Kind.HURRICANE, StormReport.Kind.SEVERE_THUNDERSTORM), true) == 4,
				"A hurricane among the rest is not level 4");
		helper.succeed();
	}
}
