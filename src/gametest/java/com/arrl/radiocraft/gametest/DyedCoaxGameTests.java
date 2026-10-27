package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.blocks.antennas.DoubleVHFAntennaBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Dyed coax: side by side runs of different colours stay apart; plain coax joins any colour. */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DyedCoaxGameTests {

	private static final BlockPos RADIO_A = new BlockPos(3, 2, 2), RADIO_B = new BlockPos(4, 2, 2);

	/** A J-pole at {@code lower}. */
	private static void jPole(GameTestHelper helper, BlockPos lower) {
		helper.setBlock(lower, RadiocraftBlocks.J_POLE_ANTENNA.get().defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.LOWER));
		helper.setBlock(lower.above(), RadiocraftBlocks.J_POLE_ANTENNA.get().defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.UPPER));
	}

	/** Two blocks of {@code wire} south of {@code radio}, up to its J-pole. */
	private static void run(GameTestHelper helper, BlockPos radio, WireBlock wire) {
		GameTestUtil.placeWire(helper, radio.south(), wire);
		GameTestUtil.placeWire(helper, radio.south(2), wire);
	}

	private static int antennas(GameTestHelper helper, BlockPos radio) {
		return GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).getAntennas().size();
	}

	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 60)
	public static void parallelRunsOfTwoColoursStayApart(GameTestHelper helper) {
		helper.setBlock(RADIO_A, RadiocraftBlocks.VHF_BASE_STATION.get());
		helper.setBlock(RADIO_B, RadiocraftBlocks.VHF_BASE_STATION.get());
		jPole(helper, RADIO_A.south(3));
		jPole(helper, RADIO_B.south(3));
		run(helper, RADIO_A, RadiocraftBlocks.COLOURED_COAX.get(DyeColor.RED).get());
		run(helper, RADIO_B, RadiocraftBlocks.COLOURED_COAX.get(DyeColor.BLUE).get());
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					helper.assertTrue(antennas(helper, RADIO_A) == 1, "the red run has " + antennas(helper, RADIO_A) + " antennas");
					helper.assertTrue(antennas(helper, RADIO_B) == 1, "the blue run has " + antennas(helper, RADIO_B) + " antennas");
					helper.assertFalse(helper.getBlockState(RADIO_A.south()).getValue(WireBlock.EAST), "the red coax is drawn joined to the blue");
					run(helper, RADIO_B, RadiocraftBlocks.COAX_WIRE.get()); // Plain now: it joins the red.
				})
				.thenIdle(2)
				.thenExecute(() -> helper.assertTrue(antennas(helper, RADIO_A) == 2, "plain coax beside red should join it, "
						+ antennas(helper, RADIO_A) + " antennas"))
				.thenSucceed();
	}
}
