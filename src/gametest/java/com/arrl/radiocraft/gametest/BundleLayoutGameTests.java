package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.blocks.WireBundleBlock;
import com.arrl.radiocraft.common.blocks.WireGeometry;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * How bundles are drawn, checked by {@link WireGeometry} (the strands' centre lines): round bends of either hand the
 * strands keep their order instead of crossing, and a run up the side of a lattice tower leg (beam, gap, beam) stays
 * in one straight line, meeting itself at every block (reported 2026-10-28).
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class BundleLayoutGameTests {

	private static final List<WireBlock> COLOURS = List.of(RadiocraftBlocks.COLOURED_COAX.get(DyeColor.ORANGE).get(),
			RadiocraftBlocks.COLOURED_COAX.get(DyeColor.LIGHT_BLUE).get(), RadiocraftBlocks.COLOURED_COAX.get(DyeColor.GREEN).get());

	/** Lays the three-colour bundle along {@code path}, one block after another. */
	private static void lay(GameTestHelper helper, List<BlockPos> path) {
		for(BlockPos pos : path) {
			GameTestUtil.placeWire(helper, pos, COLOURS.getFirst());
			for(WireBlock colour : COLOURS.subList(1, COLOURS.size()))
				WireBundleBlock.add(helper.getLevel(), helper.absolutePos(pos), colour);
		}
	}

	private static void assertClean(GameTestHelper helper, BlockPos from, BlockPos to) {
		List<String> problems = WireGeometry.check(helper.getLevel(), helper.absolutePos(from), helper.absolutePos(to));
		helper.assertTrue(problems.isEmpty(), problems.size() + " problems: " + String.join("; ", problems.subList(0, Math.min(4, problems.size()))));
	}

	/** A bundle going north, then east (the hand that used to cross), then north again, then west (the other hand). */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
	public static void bundlesKeepTheirOrderRoundBends(GameTestHelper helper) {
		for(int x = 1; x <= 14; x++)
			for(int z = 1; z <= 14; z++)
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
		lay(helper, List.of(new BlockPos(3, 2, 13), new BlockPos(3, 2, 12), new BlockPos(3, 2, 11), new BlockPos(3, 2, 10),
				new BlockPos(4, 2, 10), new BlockPos(5, 2, 10), new BlockPos(6, 2, 10),
				new BlockPos(6, 2, 9), new BlockPos(6, 2, 8), new BlockPos(6, 2, 7),
				new BlockPos(5, 2, 7), new BlockPos(4, 2, 7)));
		helper.runAfterDelay(3, () -> { // The turns are solved at the end of a tick.
			assertClean(helper, new BlockPos(2, 2, 6), new BlockPos(7, 2, 14));
			helper.succeed();
		});
	}

	/** A bundle up the side of a tower leg whose beams come and go (a lattice): straight, and meeting at every block. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
	public static void bundlesRunStraightUpALatticeLeg(GameTestHelper helper) {
		BlockPos foot = new BlockPos(6, 2, 6);
		for(int y = 2; y <= 8; y += 2) // Beam, gap, beam, gap: every other block beside the run.
			helper.setBlock(new BlockPos(6, y, 5), RadiocraftBlocks.TOWER_BEAM.get());
		helper.setBlock(foot.below(), Blocks.STONE);
		helper.setBlock(foot.south().below(), Blocks.STONE);
		List<BlockPos> path = new java.util.ArrayList<>();
		path.add(foot.south());
		for(int y = 2; y <= 8; y++)
			path.add(new BlockPos(6, y, 6));
		lay(helper, path);
		helper.runAfterDelay(3, () -> {
			for(int y = 3; y <= 7; y++) {
				WireBlock orange = COLOURS.getFirst();
				var state = orange.sameKindState(helper.getLevel(), helper.absolutePos(new BlockPos(6, y, 6)));
				helper.assertTrue(state != null && state.getValue(WireBlock.HUG) == WireBlock.Edge.NORTH, "The run at y " + y + " does not follow the leg");
			}
			assertClean(helper, new BlockPos(5, 2, 5), new BlockPos(7, 8, 7));
			helper.succeed();
		});
	}

	/**
	 * A bundle going south whose strands turn off east in an order the colours' rank does not give (green first, then
	 * orange, then light blue, as down a row of radios): the run's colour order is chosen so none crosses.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
	public static void bundlesPickAnOrderThatPeelsOffCleanly(GameTestHelper helper) {
		for(int x = 1; x <= 10; x++)
			for(int z = 1; z <= 12; z++)
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
		WireBlock orange = COLOURS.get(0), lightBlue = COLOURS.get(1), green = COLOURS.get(2);
		for(int z = 2; z <= 9; z++) {
			BlockPos pos = new BlockPos(5, 2, z);
			GameTestUtil.placeWire(helper, pos, lightBlue);
			if(z <= 7)
				WireBundleBlock.add(helper.getLevel(), helper.absolutePos(pos), orange);
			if(z <= 5)
				WireBundleBlock.add(helper.getLevel(), helper.absolutePos(pos), green);
		}
		GameTestUtil.placeWire(helper, new BlockPos(6, 2, 5), green);
		GameTestUtil.placeWire(helper, new BlockPos(6, 2, 7), orange);
		GameTestUtil.placeWire(helper, new BlockPos(6, 2, 9), lightBlue);
		helper.runAfterDelay(3, () -> {
			assertClean(helper, new BlockPos(4, 2, 1), new BlockPos(7, 2, 10));
			helper.succeed();
		});
	}

	/** A cable hanging from the ceiling beside floor wire of its kind (they could never meet) goes down to the floor. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20)
	public static void aHangingCableBesideFloorWireLiesOnTheFloor(GameTestHelper helper) {
		helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
		helper.setBlock(new BlockPos(4, 1, 3), Blocks.STONE);
		helper.setBlock(new BlockPos(8, 3, 8), Blocks.STONE);
		WireBlock coax = RadiocraftBlocks.COAX_WIRE.get();
		GameTestUtil.placeWire(helper, new BlockPos(3, 2, 3), coax);
		BlockPos beside = helper.absolutePos(new BlockPos(4, 2, 3)), alone = helper.absolutePos(new BlockPos(8, 2, 8));
		helper.getLevel().setBlockAndUpdate(beside, coax.getBlockState(helper.getLevel(), beside, true));
		helper.getLevel().setBlockAndUpdate(alone, coax.getBlockState(helper.getLevel(), alone, true));
		helper.runAfterDelay(1, () -> {
			helper.assertFalse(WireBlock.isCeiling(helper.getLevel().getBlockState(beside)), "The cable beside floor wire still hangs");
			helper.assertTrue(WireBlock.isCeiling(helper.getLevel().getBlockState(alone)), "A cable on its own under a ceiling no longer hangs");
			assertClean(helper, new BlockPos(2, 2, 2), new BlockPos(5, 2, 4));
			helper.succeed();
		});
	}
}
