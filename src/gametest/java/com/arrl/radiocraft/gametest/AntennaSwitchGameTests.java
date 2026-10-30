package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.AntennaSwitchBlockEntity;
import com.arrl.radiocraft.common.blocks.antennas.AntennaSwitchBlock;
import com.arrl.radiocraft.common.blocks.antennas.DoubleVHFAntennaBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * The antenna switch (maintainer, 2026-10-30): its input turns to the radio's coax by itself, and the radio has only
 * the antenna on the output the knob is turned to.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class AntennaSwitchGameTests {

	private static final BlockPos RADIO = new BlockPos(3, 2, 5), SWITCH = new BlockPos(5, 2, 5);
	/** The J-poles on outputs 1 (north, from a west input) and 2 (east). */
	private static final BlockPos NORTH_POLE = new BlockPos(5, 2, 3), EAST_POLE = new BlockPos(7, 2, 5);

	private static void jPole(GameTestHelper helper, BlockPos lower) {
		helper.setBlock(lower, RadiocraftBlocks.J_POLE_ANTENNA.get().defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.LOWER));
		helper.setBlock(lower.above(), RadiocraftBlocks.J_POLE_ANTENNA.get().defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.UPPER));
	}

	private static List<AntennaNetworkObject> antennas(GameTestHelper helper) {
		return GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class).getAntennas();
	}

	private static void assertOnlyAntenna(GameTestHelper helper, BlockPos pole) {
		List<AntennaNetworkObject> antennas = antennas(helper);
		helper.assertTrue(antennas.size() == 1 && antennas.getFirst().getPos().equals(helper.absolutePos(pole)),
				"The radio should have only the antenna at " + pole.toShortString() + ", has " + antennas.stream().map(a -> helper.relativePos(a.getPos()).toShortString()).toList());
	}

	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 120)
	public static void switchFacesTheRadioAndSelectsOneAntenna(GameTestHelper helper) {
		helper.setBlock(SWITCH, RadiocraftBlocks.ANTENNA_SWITCH.get()); // Input north by default: wrong on purpose.
		jPole(helper, NORTH_POLE);
		jPole(helper, EAST_POLE);
		GameTestUtil.placeWire(helper, SWITCH.north(), RadiocraftBlocks.COAX_WIRE.get());
		GameTestUtil.placeWire(helper, SWITCH.east(), RadiocraftBlocks.COAX_WIRE.get());
		helper.setBlock(RADIO, RadiocraftBlocks.VHF_BASE_STATION.get());
		GameTestUtil.placeWire(helper, RADIO.east(), RadiocraftBlocks.COAX_WIRE.get());
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(AntennaSwitchBlock.input(helper.getBlockState(SWITCH)) == Direction.WEST,
						"The input did not turn to the radio: " + AntennaSwitchBlock.input(helper.getBlockState(SWITCH))))
				.thenWaitUntil(() -> assertOnlyAntenna(helper, NORTH_POLE)) // Output 1, clockwise from west.
				.thenExecute(() -> {
					helper.setBlock(SWITCH, helper.getBlockState(SWITCH).setValue(AntennaSwitchBlock.OUTPUT, 2));
					AntennaSwitchBlockEntity.rewire(helper.getLevel(), helper.absolutePos(SWITCH));
					assertOnlyAntenna(helper, EAST_POLE);
					helper.assertTrue(GameTestUtil.networkObject(helper, NORTH_POLE, AntennaNetworkObject.class).getRadios().isEmpty(),
							"The antenna switched away from still has the radio");
				})
				.thenExecute(() -> {
					helper.setBlock(SWITCH, helper.getBlockState(SWITCH).setValue(AntennaSwitchBlock.OUTPUT, 3)); // South: nothing there.
					AntennaSwitchBlockEntity.rewire(helper.getLevel(), helper.absolutePos(SWITCH));
					helper.assertTrue(antennas(helper).isEmpty(), "Output 3 has no antenna, but the radio has " + antennas(helper).size());
				})
				.thenSucceed();
	}
}
