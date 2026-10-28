package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.blocks.WireBundleBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Dyed power wire: side by side runs of different colours stay apart; plain wire joins any colour; bundles keep the colours' rules. */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ColouredWireGameTests {

	private static final WireBlock WIRE = RadiocraftBlocks.WIRE.get();
	private static final WireBlock RED = RadiocraftBlocks.COLOURED_WIRE.get(DyeColor.RED).get();
	private static final WireBlock BLUE = RadiocraftBlocks.COLOURED_WIRE.get(DyeColor.BLUE).get();
	private static final BlockPos RADIO_A = new BlockPos(3, 2, 2), RADIO_B = new BlockPos(4, 2, 2);
	private static final BlockPos BATTERY_A = RADIO_A.south(3), BATTERY_B = RADIO_B.south(3);

	/** Two blocks of {@code wire} south of {@code radio}, up to its battery. */
	private static void run(GameTestHelper helper, BlockPos radio, WireBlock wire) {
		GameTestUtil.placeWire(helper, radio.south(), wire);
		GameTestUtil.placeWire(helper, radio.south(2), wire);
	}

	/** True if the radio at {@code radio} is on a power network with the battery at {@code battery}. */
	private static boolean wiredTo(GameTestHelper helper, BlockPos radio, BlockPos battery) {
		BatteryNetworkObject cell = GameTestUtil.networkObject(helper, battery, BatteryNetworkObject.class);
		for(BENetwork network : GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).getNetworks().values())
			if(network.getNetworkObjects().contains(cell))
				return true;
		return false;
	}

	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 60)
	public static void parallelRunsOfTwoColoursStayApart(GameTestHelper helper) {
		helper.setBlock(RADIO_A, RadiocraftBlocks.VHF_BASE_STATION.get());
		helper.setBlock(RADIO_B, RadiocraftBlocks.VHF_BASE_STATION.get());
		helper.setBlock(BATTERY_A, RadiocraftBlocks.LARGE_BATTERY.get());
		helper.setBlock(BATTERY_B, RadiocraftBlocks.LARGE_BATTERY.get());
		run(helper, RADIO_A, RED);
		run(helper, RADIO_B, BLUE);
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					helper.assertTrue(wiredTo(helper, RADIO_A, BATTERY_A), "the red run does not reach its own battery");
					helper.assertTrue(wiredTo(helper, RADIO_B, BATTERY_B), "the blue run does not reach its own battery");
					helper.assertFalse(wiredTo(helper, RADIO_A, BATTERY_B), "the red run joined the blue one");
					helper.assertFalse(helper.getBlockState(RADIO_A.south()).getValue(WireBlock.EAST), "the red wire is drawn joined to the blue");
					run(helper, RADIO_B, WIRE); // Plain now: it joins the red.
				})
				.thenIdle(2)
				.thenExecute(() -> {
					helper.assertTrue(wiredTo(helper, RADIO_A, BATTERY_B), "plain wire beside red should join it");
					// Bundles: colours that keep apart may share a block; a wire that would join one already there may not.
					helper.assertTrue(WireBundleBlock.canAdd(helper.getLevel(), helper.absolutePos(RADIO_A.south()), BLUE), "red and blue wire cannot share a block");
					helper.assertFalse(WireBundleBlock.canAdd(helper.getLevel(), helper.absolutePos(RADIO_A.south()), WIRE), "plain wire went in beside red (one shorted conductor)");
					helper.assertFalse(WireBundleBlock.canAdd(helper.getLevel(), helper.absolutePos(RADIO_B.south()), RED), "red wire went in beside plain");
				})
				.thenSucceed();
	}
}
