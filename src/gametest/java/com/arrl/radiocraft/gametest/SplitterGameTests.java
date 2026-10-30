package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.arrl.radiocraft.gametest.TransmissionGameTests.*;

/**
 * The receive-only antenna splitter (maintainer, 2026-10-27): two radios behind it on one dipole both hear it, each at
 * 1/sqrt(2) of the signal, where without it they would overdraw the antenna; neither can transmit through it. Station A
 * of {@link TransmissionGameTests} talks; B's dipole feeds the splitter.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SplitterGameTests {

	/** A frequency of its own (see TransmissionGameTests). */
	private static final int F_SPLIT = 350;
	private static final BlockPos SPLITTER = new BlockPos(8, 2, 14);
	private static final BlockPos LEFT = new BlockPos(6, 2, 14), RIGHT = new BlockPos(10, 2, 14);

	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
	public static void splitterFeedsTwoReceivers(GameTestHelper helper) {
		buildStation(helper, RADIO_A, BALUN_A);
		helper.setBlock(BALUN_B, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
		GameTestUtil.wire(helper, BALUN_B, BALUN_B.west(5));
		GameTestUtil.wire(helper, BALUN_B, BALUN_B.east(5));
		GameTestUtil.placeWire(helper, BALUN_B.south(), RadiocraftBlocks.COAX_WIRE.get());
		helper.setBlock(SPLITTER, RadiocraftBlocks.ANTENNA_SPLITTER.get());
		for(BlockPos radio : List.of(LEFT, RIGHT)) {
			helper.setBlock(radio, RadiocraftBlocks.HF_RADIO_20M.get());
			GameTestUtil.placeWire(helper, radio.getX() < SPLITTER.getX() ? radio.east() : radio.west(), RadiocraftBlocks.COAX_WIRE.get());
		}
		helper.startSequence()
				.thenWaitUntil(() -> {
					for(BlockPos radio : List.of(RADIO_A, LEFT, RIGHT)) {
						var antennas = GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).getAntennas();
						helper.assertTrue(antennas.size() == 1 && antennas.getFirst().getAntenna() != null, "Radio at " + radio.toShortString() + " has no antenna yet");
					}
				})
				.thenExecute(() -> {
					AntennaNetworkObject dipole = GameTestUtil.networkObject(helper, BALUN_B, AntennaNetworkObject.class);
					helper.assertTrue(dipole.getRadios().size() == 2, "Expected both radios behind the splitter, found " + dipole.getRadios().size());
					helper.assertTrue(dipole.isSplit() && !dipole.isOverdrawn(), "Two radios behind a splitter overdraw the antenna");
					setupSsb(helper, RADIO_A, F_SPLIT);
					setupSsb(helper, LEFT, F_SPLIT);
					setupSsb(helper, RIGHT, F_SPLIT);
					speakInto(helper, RADIO_A);
					double share = EXPECTED_VOICE_STRENGTH / Math.sqrt(2.0D);
					assertHeard(helper, LEFT, share);
					assertHeard(helper, RIGHT, share);
				})
				.thenIdle(2)
				.thenExecute(() -> {
					long before = radio(helper, RADIO_A).getLastSignalGameTime();
					speakInto(helper, LEFT); // Receive only: nothing goes out.
					helper.assertTrue(radio(helper, RADIO_A).getLastSignalGameTime() == before, "A heard a radio transmitting through the splitter");
				})
				.thenSucceed();
	}
}
