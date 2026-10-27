package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Two radios on one antenna's coax overdraw it and neither hears anything; the radio says so (COAX SHARED). Breaking
 * and re-placing the second radio, or cutting the coax between them, must leave no stale radio on the antenna.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CoaxSharedGameTests {

	private static final BlockPos RADIO_A = new BlockPos(3, 2, 2), RADIO_B = new BlockPos(5, 2, 2), BRIDGE = new BlockPos(4, 2, 3);

	private static RadioBlockEntity radio(GameTestHelper helper, BlockPos pos) {
		return GameTestUtil.blockEntity(helper, pos, RadioBlockEntity.class);
	}

	private static void powerOn(GameTestHelper helper, BlockPos pos) {
		if(!GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class).isPowered)
			radio(helper, pos).toggle();
	}

	/** Radios on the J-pole's coax, as its antenna counts them. */
	private static int radiosOnAntenna(GameTestHelper helper) {
		return GameTestUtil.networkObject(helper, RADIO_A.south(2), AntennaNetworkObject.class).getRadios().size();
	}

	private static void placeB(GameTestHelper helper) {
		helper.setBlock(RADIO_B, RadiocraftBlocks.VHF_BASE_STATION.get());
	}

	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 120)
	public static void sharedCoaxIsShownAndClearsWhenSplit(GameTestHelper helper) {
		GameTestUtil.radioWithJPole(helper, RADIO_A); // Coax at A.south(), the J-pole at A.south(2).
		GameTestUtil.placeWire(helper, BRIDGE, RadiocraftBlocks.COAX_WIRE.get());
		GameTestUtil.placeWire(helper, RADIO_B.south(), RadiocraftBlocks.COAX_WIRE.get());
		placeB(helper);
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					powerOn(helper, RADIO_A);
					powerOn(helper, RADIO_B);
				})
				.thenIdle(4)
				.thenExecute(() -> {
					helper.assertTrue(radiosOnAntenna(helper) == 2, "both radios should be on the J-pole's coax, found " + radiosOnAntenna(helper));
					helper.assertTrue(radio(helper, RADIO_A).isCoaxShared(), "radio A should say its coax is shared");
					helper.setBlock(RADIO_B, Blocks.AIR); // Broken: it must leave the antenna.
				})
				.thenIdle(4)
				.thenExecute(() -> {
					helper.assertTrue(radiosOnAntenna(helper) == 1, "a broken radio stayed on the antenna: " + radiosOnAntenna(helper));
					helper.assertFalse(radio(helper, RADIO_A).isCoaxShared(), "radio A still says shared with B gone");
					placeB(helper); // Re-placed on the same spot: shared again, and only once.
				})
				.thenIdle(4)
				.thenExecute(() -> {
					helper.assertTrue(radiosOnAntenna(helper) == 2, "re-placed radio counted " + radiosOnAntenna(helper) + " times with A");
					helper.setBlock(BRIDGE, Blocks.AIR); // Cut the coax between them.
				})
				.thenIdle(4)
				.thenExecute(() -> {
					helper.assertTrue(radiosOnAntenna(helper) == 1, "cutting the coax left " + radiosOnAntenna(helper) + " radios on the antenna");
					helper.assertFalse(radio(helper, RADIO_A).isCoaxShared(), "radio A still says shared after the cut");
				})
				.thenSucceed();
	}
}
