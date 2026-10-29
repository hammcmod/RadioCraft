package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blockentities.CommercialRadioModuleBlockEntity;
import com.arrl.radiocraft.common.blocks.StackedBlocks;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.CommercialStations;
import com.arrl.radiocraft.common.radio.TransmitterLog;
import com.arrl.radiocraft.common.radio.voice.StationProgram;
import com.arrl.radiocraft.common.worldgen.SitePiece;
import com.arrl.radiocraft.common.worldgen.SiteVariant;
import com.arrl.radiocraft.gametest.sites.SiteCheck;
import com.arrl.radiocraft.gametest.sites.SiteTemplates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The radio sites the world generator places, and the commercial radio module stations that stay on the air without
 * their chunks: every site works as found, Long Lines keeps one to four horns, and a site's module station is
 * predicted just as the site will have it.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SiteGameTests {

	/** Room for the biggest site (Long Lines, 19 by 42 by 13). */
	private static final String SITE = "empty_site";
	private static final String SITES_BATCH = "radio_sites";

	@BeforeBatch(batch = SITES_BATCH)
	public static void powerRequired(ServerLevel level) {
		RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(true);
	}

	@AfterBatch(batch = SITES_BATCH)
	public static void powerFree(ServerLevel level) {
		RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(false);
	}

	private static StructureTemplate template(GameTestHelper helper, String name) {
		return helper.getLevel().getStructureManager().get(Radiocraft.id("sites/" + name))
				.orElseThrow(() -> new IllegalStateException("no template sites/" + name));
	}

	/** Puts site {@code name} down with all its parts and waits for every station in it to be working. */
	private static void siteWorks(GameTestHelper helper, String name, boolean powered) {
		ServerLevel level = helper.getLevel();
		level.setDayTime(6000L); // Noon.
		BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
		StructureTemplate template = template(helper, name);
		template.placeInWorld(level, origin, origin, SitePiece.settings(), level.getRandom(), Block.UPDATE_ALL);
		Vec3i size = template.getSize();
		BlockPos far = origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
		helper.succeedWhen(() -> {
			SiteCheck.Report report = SiteCheck.check(level, origin, far, powered);
			helper.assertTrue(report.problems().isEmpty(), name + ": " + String.join("; ", report.problems()));
		});
	}

	@GameTest(template = SITE, batch = SITES_BATCH, timeoutTicks = 200)
	public static void repeaterSiteWorks(GameTestHelper helper) {
		siteWorks(helper, "repeater_site", true);
	}

	@GameTest(template = SITE, batch = SITES_BATCH, timeoutTicks = 200)
	public static void buriedSiteIsWiredUp(GameTestHelper helper) {
		siteWorks(helper, "buried_site", false);
	}

	@GameTest(template = SITE, batch = SITES_BATCH, timeoutTicks = 200)
	public static void ionosphericObservatoryWorks(GameTestHelper helper) {
		siteWorks(helper, "ionospheric_observatory", true);
	}

	@GameTest(template = SITE, batch = SITES_BATCH, timeoutTicks = 200)
	public static void weatherObservatoryWorks(GameTestHelper helper) {
		siteWorks(helper, "weather_observatory", true);
	}

	@GameTest(template = SITE, batch = SITES_BATCH, timeoutTicks = 200)
	public static void directionFindingStationWorks(GameTestHelper helper) {
		siteWorks(helper, "direction_finding_station", true);
	}

	@GameTest(template = SITE, batch = SITES_BATCH, timeoutTicks = 200)
	public static void longLinesWorks(GameTestHelper helper) {
		siteWorks(helper, "long_lines", true);
	}

	private static int horns(List<BoundingBox> removed) {
		int[][] horns = { { 1, 33, 2 }, { 5, 33, 2 }, { 1, 33, 8 }, { 5, 33, 8 } };
		int kept = 0;
		for(int[] horn : horns)
			if(removed.stream().noneMatch(box -> box.isInside(new BlockPos(horn[0], horn[1], horn[2]))))
				kept++;
		return kept;
	}

	/** Copies of Long Lines here and there keep one to four horns, not always the same number; one placed has as many as worked out. */
	@GameTest(template = SITE, timeoutTicks = 20)
	public static void longLinesKeepsOneToFourHorns(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<SiteVariant> variants = SiteTemplates.variants(level, "long_lines");
		helper.assertTrue(!variants.isEmpty(), "long_lines has no variants");
		Set<Integer> counts = new HashSet<>();
		for(int i = 0; i < 64; i++) {
			int kept = horns(SiteVariant.removed(variants, level.getSeed(), new BlockPos(i * 448, 140, i * -272)));
			helper.assertTrue(kept >= 1 && kept <= 4, kept + " horns");
			counts.add(kept);
		}
		helper.assertTrue(counts.size() >= 3, "only ever " + counts + " horns");
		BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
		template(helper, "long_lines").placeInWorld(level, origin, origin, SitePiece.settings(variants), level.getRandom(), Block.UPDATE_ALL);
		int expected = horns(SiteVariant.removed(variants, level.getSeed(), origin));
		helper.runAfterDelay(2, () -> {
			int horns = 0, radios = 0;
			for(BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(18, 41, 12))) {
				BlockEntity entity = level.getBlockEntity(pos);
				if(entity == null)
					continue;
				ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType());
				if(id != null && id.getPath().equals("microwave_horn"))
					horns++;
				if(id != null && id.getPath().equals("microwave_radio"))
					radios++;
			}
			helper.assertTrue(horns == expected, horns + " horns placed, " + expected + " worked out");
			helper.assertTrue(radios == expected, radios + " microwave radios for " + expected + " horns");
			helper.succeed();
		});
	}

	/** The station worked out for a site's module is the one the module has once the site is placed. */
	@GameTest(template = SITE, timeoutTicks = 40)
	public static void predictedModuleIsThePlacedOne(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
		SitePiece piece = new SitePiece(level.getStructureManager(), Radiocraft.id("sites/repeater_site"), origin, List.of());
		List<CommercialStations.Station> predicted = piece.stations(level);
		helper.assertTrue(predicted.size() == 1, predicted.size() + " stations worked out for the repeater site");
		piece.template().placeInWorld(level, origin, origin, piece.placeSettings(), level.getRandom(), Block.UPDATE_ALL);
		CommercialStations.Station expected = predicted.getFirst();
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(level.getBlockEntity(expected.pos()) instanceof CommercialRadioModuleBlockEntity, "no module at " + expected.pos().toShortString());
			CommercialRadioModuleBlockEntity module = (CommercialRadioModuleBlockEntity)level.getBlockEntity(expected.pos());
			CommercialStations.Station placed = module.station();
			helper.assertTrue(placed != null && placed.equals(new CommercialStations.Station(expected.pos(), expected.kind(), expected.band(), expected.frequency(),
					expected.seed(), expected.callsign(), false)), "predicted " + expected + ", placed " + placed);
			helper.assertTrue(CommercialStations.get(level).get(expected.pos()) != null, "the placed module is not on the air");
			helper.succeed();
		});
	}

	/** A station whose chunk is not loaded (nothing there at all) is on the air all the same. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100)
	public static void stationSendsWithItsChunkUnloaded(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos far = helper.absolutePos(new BlockPos(2, 2, 2)).offset(200_000, 0, 0);
		float hz = 147_555_000.0F;
		helper.assertTrue(!level.isLoaded(far), "the far spot is loaded");
		CommercialStations.get(level).register(new CommercialStations.Station(far, StationProgram.Kind.DF_BEEPER, "2m", hz, 1L, "K0TST", false));
		helper.succeedWhen(() -> {
			helper.assertTrue(TransmitterLog.nearest(level, far, hz, 1000.0D, 64.0D, 20) != null, "nothing on the air from the unloaded station");
			CommercialStations.get(level).remove(far);
		});
	}

	/** A module whose chunk unloads stays on the air; one broken goes off it. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
	public static void moduleStaysOnTheAirUnloadedNotBroken(GameTestHelper helper) {
		BlockPos bottom = new BlockPos(4, 2, 4), other = new BlockPos(10, 2, 10);
		for(BlockPos at : new BlockPos[] { bottom, other })
			for(int i = 0; i < StackedBlocks.HEIGHT; i++)
				helper.setBlock(at.above(i), RadiocraftBlocks.COMMERCIAL_RADIO_MODULE.get().defaultBlockState().setValue(StackedBlocks.PART, i));
		ServerLevel level = helper.getLevel();
		helper.runAfterDelay(3, () -> {
			CommercialStations stations = CommercialStations.get(level);
			helper.assertTrue(stations.get(helper.absolutePos(bottom)) != null && stations.get(helper.absolutePos(other)) != null, "the modules are not on the air");
			CommercialRadioModuleBlockEntity module = GameTestUtil.blockEntity(helper, other, CommercialRadioModuleBlockEntity.class);
			module.onChunkUnloaded();
			module.setRemoved();
			helper.assertTrue(stations.get(helper.absolutePos(other)) != null, "an unloaded module went off the air");
			level.setBlock(helper.absolutePos(bottom), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			helper.assertTrue(stations.get(helper.absolutePos(bottom)) == null, "a broken module is still on the air");
			stations.remove(helper.absolutePos(other));
			helper.succeed();
		});
	}

	/** A station struck off when its chunk is loaded and its module is not there (broken while unloaded, or predicted wrong). */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 60)
	public static void missingModuleIsStruckOff(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(6, 2, 6));
		CommercialStations.get(level).register(new CommercialStations.Station(pos, StationProgram.Kind.CW_BEACON, Band.getBand("40m").name(), 7_050_000.0F,
				2L, "K0TST", true));
		helper.succeedWhen(() -> helper.assertTrue(CommercialStations.get(level).get(pos) == null, "a station with no module is still on the air"));
	}
}
