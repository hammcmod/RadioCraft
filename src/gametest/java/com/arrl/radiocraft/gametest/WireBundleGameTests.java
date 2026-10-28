package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.WireBundleBlockEntity;
import com.arrl.radiocraft.common.blocks.Strand;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.blocks.WireBundleBlock;
import com.arrl.radiocraft.common.blocks.antennas.DoubleVHFAntennaBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Wire bundles: several cables in one block ({@link WireBundleBlock}). Each strand keeps its own kind's links, so
 * power and coax share a block without meeting, two colours of coax stay apart, and taking a strand out splits only
 * its network.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WireBundleGameTests {

	private static final WireBlock WIRE = RadiocraftBlocks.WIRE.get();
	private static final WireBlock COAX = RadiocraftBlocks.COAX_WIRE.get();
	private static final WireBlock RED = RadiocraftBlocks.COLOURED_COAX.get(DyeColor.RED).get();
	private static final WireBlock BLUE = RadiocraftBlocks.COLOURED_COAX.get(DyeColor.BLUE).get();

	private static void jPole(GameTestHelper helper, BlockPos lower) {
		helper.setBlock(lower, RadiocraftBlocks.J_POLE_ANTENNA.get().defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.LOWER));
		helper.setBlock(lower.above(), RadiocraftBlocks.J_POLE_ANTENNA.get().defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.UPPER));
	}

	/** Lays {@code first} at {@code pos} as a player would, then the other kinds in with it. */
	private static void bundle(GameTestHelper helper, BlockPos pos, WireBlock first, WireBlock... more) {
		GameTestUtil.placeWire(helper, pos, first);
		for(WireBlock kind : more) {
			helper.assertTrue(WireBundleBlock.canAdd(helper.getLevel(), helper.absolutePos(pos), kind), kind + " cannot be added at " + pos.toShortString());
			WireBundleBlock.add(helper.getLevel(), helper.absolutePos(pos), kind);
		}
	}

	private static List<Strand> strands(GameTestHelper helper, BlockPos pos) {
		return Strand.at(helper.getLevel(), helper.absolutePos(pos));
	}

	private static int antennas(GameTestHelper helper, BlockPos radio) {
		return GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).getAntennas().size();
	}

	/**
	 * A radio's coax and its power wire leave by the same two blocks: the coax reaches the J-pole, the wire the battery,
	 * and neither sees the other's network.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 80)
	public static void coaxAndPowerShareABlock(GameTestHelper helper) {
		BlockPos radio = new BlockPos(3, 2, 2);
		BlockPos battery = new BlockPos(5, 2, 4);
		helper.setBlock(radio, RadiocraftBlocks.VHF_BASE_STATION.get());
		jPole(helper, radio.south(3));
		helper.setBlock(battery, RadiocraftBlocks.LARGE_BATTERY.get());
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					bundle(helper, radio.south(), COAX, WIRE);
					bundle(helper, radio.south(2), COAX, WIRE);
					GameTestUtil.placeWire(helper, radio.south(2).east(), WIRE);
					helper.assertTrue(helper.getBlockState(radio.south()).is(RadiocraftBlocks.WIRE_BUNDLE.get()), "the first block is not a bundle");
					helper.assertTrue(strands(helper, radio.south(2)).size() == 2, "the second bundle has " + strands(helper, radio.south(2)).size() + " strands");
					helper.assertFalse(WireBundleBlock.canAdd(helper.getLevel(), helper.absolutePos(radio.south()), RadiocraftBlocks.WIRE.get()), "a second power wire went in");
					helper.assertFalse(WireBundleBlock.canAdd(helper.getLevel(), helper.absolutePos(radio.south()), (WireBlock)RadiocraftBlocks.WATERPROOF_WIRE.get()), "two power wires in one block");
					helper.assertTrue(WireBundleBlock.canAdd(helper.getLevel(), helper.absolutePos(radio.south()), RED), "a second coax colour cannot go in");
				})
				.thenIdle(2)
				.thenExecute(() -> {
					helper.assertTrue(antennas(helper, radio) == 1, "the coax strand reaches " + antennas(helper, radio) + " antennas");
					RadioNetworkObject network = GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class);
					helper.assertTrue(network.hasBattery(), "the power strand does not reach the battery");
					GameTestUtil.networkObject(helper, battery, BatteryNetworkObject.class).getStorage().setEnergy(100_000);
					GameTestUtil.blockEntity(helper, radio, com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity.class).toggle();
				})
				.thenIdle(3)
				.thenExecute(() -> helper.assertTrue(GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).isPowered, "the radio did not power up over the bundle's wire"))
				.thenSucceed();
	}

	/**
	 * Two radios' runs, each fed by a block of its own colour (a radio touching the bundle itself would take every
	 * coax in it, as it would two coax blocks), share two bundle blocks and reach their own antennas only. Taking the
	 * blue strand out of the first block cuts the blue radio off; putting it back joins it up again; the red run never
	 * notices.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100)
	public static void twoColoursInOneBundleStayApart(GameTestHelper helper) {
		BlockPos radioRed = new BlockPos(3, 2, 1), radioBlue = new BlockPos(5, 2, 3);
		BlockPos first = new BlockPos(3, 2, 3), second = new BlockPos(3, 2, 4);
		helper.setBlock(radioRed, RadiocraftBlocks.VHF_BASE_STATION.get());
		helper.setBlock(radioBlue, RadiocraftBlocks.VHF_BASE_STATION.get());
		jPole(helper, new BlockPos(1, 2, 4)); // Red: west out of the second block.
		jPole(helper, new BlockPos(3, 2, 6)); // Blue: on south.
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					GameTestUtil.placeWire(helper, radioRed.south(), RED); // Into the first block from the north...
					GameTestUtil.placeWire(helper, radioBlue.west(), BLUE); // ...and from the east.
					bundle(helper, first, RED, BLUE);
					bundle(helper, second, RED, BLUE);
					GameTestUtil.placeWire(helper, second.west(), RED);
					GameTestUtil.placeWire(helper, second.south(), BLUE);
				})
				.thenIdle(2)
				.thenExecute(() -> {
					helper.assertTrue(antennas(helper, radioRed) == 1, "the red radio sees " + antennas(helper, radioRed) + " antennas");
					helper.assertTrue(antennas(helper, radioBlue) == 1, "the blue radio sees " + antennas(helper, radioBlue) + " antennas");
					WireBundleBlockEntity entity = GameTestUtil.blockEntity(helper, first, WireBundleBlockEntity.class);
					helper.assertTrue(entity.strand(RED) != null && entity.strand(RED).getValue(WireBlock.NORTH), "the red strand is not drawn to the red radio");
					helper.assertFalse(entity.strand(RED).getValue(WireBlock.EAST), "the red strand is drawn to the blue radio");
					helper.assertTrue(entity.strand(BLUE).getValue(WireBlock.EAST), "the blue strand is not drawn to the blue radio");
					helper.assertFalse(entity.strand(BLUE).getValue(WireBlock.NORTH), "the blue strand is drawn to the red radio");
					helper.assertTrue(WireBundleBlock.remove(helper.getLevel(), helper.absolutePos(first), BLUE) != null, "the blue strand did not come out");
					helper.assertTrue(helper.getBlockState(first).is(RED), "down to one strand it should be a red coax block again");
				})
				.thenIdle(2)
				.thenExecute(() -> {
					helper.assertTrue(antennas(helper, radioBlue) == 0, "the blue radio still sees " + antennas(helper, radioBlue) + " antennas with its strand gone");
					helper.assertTrue(antennas(helper, radioRed) == 1, "the red radio lost its antenna");
					WireBundleBlock.add(helper.getLevel(), helper.absolutePos(first), BLUE);
				})
				.thenIdle(2)
				.thenExecute(() -> helper.assertTrue(antennas(helper, radioBlue) == 1, "the blue radio does not see its antenna again, " + antennas(helper, radioBlue)))
				.thenSucceed();
	}

	/** A bundle's coax strand climbs a step to the J-pole on it while its power strand runs on along the floor to the battery. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 80)
	public static void bundleStrandsClimbOnTheirOwn(GameTestHelper helper) {
		BlockPos radio = new BlockPos(3, 2, 2);
		BlockPos foot = radio.south(), step = radio.south(2), top = step.above();
		BlockPos battery = new BlockPos(5, 2, 3);
		helper.setBlock(radio, RadiocraftBlocks.VHF_BASE_STATION.get());
		helper.setBlock(step, Blocks.STONE);
		jPole(helper, top);
		helper.setBlock(battery, RadiocraftBlocks.LARGE_BATTERY.get());
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					GameTestUtil.placeWire(helper, foot.east(), WIRE);
					bundle(helper, foot, COAX, WIRE);
					WireBundleBlockEntity entity = GameTestUtil.blockEntity(helper, foot, WireBundleBlockEntity.class);
					helper.assertTrue(entity.strand(COAX).getValue(WireBlock.CLIMB) != WireBlock.Climb.NONE, "the coax strand does not climb the step");
					helper.assertTrue(entity.strand(WIRE).getValue(WireBlock.CLIMB) == WireBlock.Climb.NONE, "the power strand climbs to a J-pole");
					helper.assertTrue(entity.strand(WIRE).getValue(WireBlock.EAST), "the power strand does not run east to its wire");
				})
				.thenIdle(2)
				.thenExecute(() -> {
					helper.assertTrue(antennas(helper, radio) == 1, "the climbing coax strand reaches " + antennas(helper, radio) + " antennas");
					helper.assertTrue(GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).hasBattery(), "the power strand does not reach the battery");
				})
				.thenSucceed();
	}
}
