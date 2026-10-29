package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A microwave radio runs off a power wire, beside it or under it (reported 2026-10-28: wire run under a row of radios
 * showed a link to each but none would power on). Power draw is on for this batch only.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class MicrowavePowerGameTests {

	private static final String BATCH = "power_microwave";

	@BeforeBatch(batch = BATCH)
	public static void powerRequired(ServerLevel level) {
		RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(true);
	}

	@AfterBatch(batch = BATCH)
	public static void powerFree(ServerLevel level) {
		RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(false);
	}

	/** Two radios: one with the wire running under it, one with the wire beside it; both on one charged battery. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = BATCH)
	public static void radioRunsOffAWireUnderIt(GameTestHelper helper) {
		BlockPos battery = new BlockPos(3, 2, 3);
		BlockPos under = new BlockPos(5, 3, 3), beside = new BlockPos(8, 2, 4);
		for(int x = 3; x <= 9; x++)
			helper.setBlock(new BlockPos(x, 1, 3), Blocks.STONE);
		helper.setBlock(battery, RadiocraftBlocks.LARGE_BATTERY.get());
		for(int x = 4; x <= 8; x++)
			GameTestUtil.placeWire(helper, new BlockPos(x, 2, 3), RadiocraftBlocks.WIRE.get());
		helper.setBlock(under, RadiocraftBlocks.MICROWAVE_RADIO.get()); // Standing on the wire's block.
		helper.setBlock(beside, RadiocraftBlocks.MICROWAVE_RADIO.get());
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> {
					GameTestUtil.networkObject(helper, battery, BatteryNetworkObject.class).getStorage().setEnergy(100_000);
					for(BlockPos at : new BlockPos[] { under, beside }) {
						MicrowaveRadioBlockEntity radio = GameTestUtil.blockEntity(helper, at, MicrowaveRadioBlockEntity.class);
						if(!radio.isPowered())
							radio.togglePower();
					}
				})
				.thenWaitUntil(() -> {
					helper.assertTrue(GameTestUtil.blockEntity(helper, beside, MicrowaveRadioBlockEntity.class).isPowered(), "The radio beside the wire did not power on");
					helper.assertTrue(GameTestUtil.blockEntity(helper, under, MicrowaveRadioBlockEntity.class).isPowered(), "The radio on top of the wire did not power on");
				})
				.thenSucceed();
	}

	/**
	 * The same with the wire under the radio in a bundle with coax to a dish, as cable runs often are: power and coax
	 * come into the same face, and the radio must be on both networks there (it had one slot a side, 2026-10-28).
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = BATCH)
	public static void radioRunsOffABundleUnderIt(GameTestHelper helper) {
		BlockPos battery = new BlockPos(3, 2, 6), radio = new BlockPos(6, 3, 6);
		for(int x = 3; x <= 8; x++)
			helper.setBlock(new BlockPos(x, 1, 6), Blocks.STONE);
		helper.setBlock(battery, RadiocraftBlocks.LARGE_BATTERY.get());
		for(int x = 4; x <= 7; x++)
			GameTestUtil.placeWire(helper, new BlockPos(x, 2, 6), RadiocraftBlocks.WIRE.get());
		for(int x = 4; x <= 7; x++) // Black coax laid in along the same run: every block a bundle.
			com.arrl.radiocraft.common.blocks.WireBundleBlock.add(helper.getLevel(), helper.absolutePos(new BlockPos(x, 2, 6)),
					RadiocraftBlocks.COLOURED_COAX.get(net.minecraft.world.item.DyeColor.BLACK).get());
		helper.setBlock(radio, RadiocraftBlocks.MICROWAVE_RADIO.get());
		helper.setBlock(new BlockPos(8, 2, 6), RadiocraftBlocks.SATELLITE_DISH.get()); // The coax goes somewhere: it makes a network too.
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> {
					helper.assertTrue(helper.getBlockState(radio.below()).getBlock() instanceof com.arrl.radiocraft.common.blocks.WireBundleBlock, "No bundle under the radio");
					GameTestUtil.networkObject(helper, battery, BatteryNetworkObject.class).getStorage().setEnergy(100_000);
					MicrowaveRadioBlockEntity be = GameTestUtil.blockEntity(helper, radio, MicrowaveRadioBlockEntity.class);
					if(!be.isPowered())
						be.togglePower();
				})
				.thenWaitUntil(() -> helper.assertTrue(GameTestUtil.blockEntity(helper, radio, MicrowaveRadioBlockEntity.class).isPowered(),
						"The radio on a bundle did not power on: networks " + com.arrl.radiocraft.api.capabilities.IBENetworks.getObject(helper.getLevel(), helper.absolutePos(radio)).getNetworks()))
				.thenSucceed();
	}
}
