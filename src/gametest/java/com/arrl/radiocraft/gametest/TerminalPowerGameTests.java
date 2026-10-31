package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.AprsTerminalBlockEntity;
import com.arrl.radiocraft.common.blocks.AprsTerminalBlock.Display;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The H19 terminal runs off its station: on while a powered radio is reachable through the station gear, with no wire
 * of its own, and dark once that radio browns out. The GameTest server runs with power draw off
 * ({@link GameTestDefaults}); on for this batch.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class TerminalPowerGameTests {

	private static final String BATCH = "power_terminal";
	private static final BlockPos RADIO = new BlockPos(3, 2, 6), IFACE = RADIO.north(), TERMINAL = IFACE.north();
	private static final BlockPos RADIO_BATTERY = RADIO.east(2);

	@BeforeBatch(batch = BATCH)
	public static void powerRequired(ServerLevel level) {
		RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(true);
	}

	@AfterBatch(batch = BATCH)
	public static void powerFree(ServerLevel level) {
		RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(false);
	}

	private static AprsTerminalBlockEntity terminal(GameTestHelper helper) {
		return GameTestUtil.blockEntity(helper, TERMINAL, AprsTerminalBlockEntity.class);
	}

	private static void charge(GameTestHelper helper, BlockPos battery) {
		GameTestUtil.networkObject(helper, battery, BatteryNetworkObject.class).getStorage().setEnergy(100_000);
	}

	/** Radio, TNC, H19 in a row; only the radio on a charged battery. The terminal lights with it and goes dark with it. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 120, batch = BATCH)
	public static void terminalRunsOffItsPoweredRadio(GameTestHelper helper) {
		helper.setBlock(RADIO, RadiocraftBlocks.VHF_BASE_STATION.get());
		helper.setBlock(IFACE, RadiocraftBlocks.RADIO_INTERFACE.get());
		helper.setBlock(TERMINAL, RadiocraftBlocks.APRS_TERMINAL.get());
		GameTestUtil.placeWire(helper, RADIO.east(), RadiocraftBlocks.WIRE.get());
		helper.setBlock(RADIO_BATTERY, RadiocraftBlocks.LARGE_BATTERY.get());
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					charge(helper, RADIO_BATTERY);
					if(!GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class).wantsOn)
						GameTestUtil.blockEntity(helper, RADIO, RadioBlockEntity.class).toggle();
				})
				.thenWaitUntil(() -> helper.assertTrue(GameTestUtil.blockEntity(helper, RADIO, RadioBlockEntity.class).isPowered(), "the radio did not come on"))
				.thenWaitUntil(() -> helper.assertTrue(terminal(helper).isRadioOn(), "the terminal does not see the powered radio through the TNC"))
				.thenExecute(() -> {
					helper.assertTrue(terminal(helper).isOn() && terminal(helper).isStationFed(), "the terminal stays dark beside a powered radio with no wire of its own");
					helper.assertFalse(terminal(helper).isPowered(), "something fed the terminal's own wire");
					helper.assertTrue(terminal(helper).getDisplay() != Display.OFF, "its screen shows " + terminal(helper).getDisplay());
					helper.assertTrue(terminal(helper).isBootPending(), "coming on did not queue a boot");
					GameTestUtil.networkObject(helper, RADIO_BATTERY, BatteryNetworkObject.class).getStorage().setEnergy(0); // The radio browns out...
				})
				.thenWaitUntil(() -> helper.assertFalse(terminal(helper).isOn(), "...and the terminal should go dark with no powered radio to talk to"))
				.thenSucceed();
	}

	/** Cabled to the TNC only by data wire, the terminal does not touch its station: it needs its own power wire. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 160, batch = BATCH)
	public static void dataWiredTerminalNeedsItsOwnPower(GameTestHelper helper) {
		BlockPos terminalPos = IFACE.north(4), terminalBattery = terminalPos.east(2);
		helper.setBlock(RADIO, RadiocraftBlocks.VHF_BASE_STATION.get());
		helper.setBlock(IFACE, RadiocraftBlocks.RADIO_INTERFACE.get());
		for(int i = 1; i <= 3; i++)
			GameTestUtil.placeWire(helper, IFACE.north(i), RadiocraftBlocks.DATA_WIRE.get());
		helper.setBlock(terminalPos, RadiocraftBlocks.APRS_TERMINAL.get());
		GameTestUtil.placeWire(helper, RADIO.east(), RadiocraftBlocks.WIRE.get());
		helper.setBlock(RADIO_BATTERY, RadiocraftBlocks.LARGE_BATTERY.get());
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					charge(helper, RADIO_BATTERY);
					if(!GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class).wantsOn)
						GameTestUtil.blockEntity(helper, RADIO, RadioBlockEntity.class).toggle();
				})
				.thenWaitUntil(() -> helper.assertTrue(GameTestUtil.blockEntity(helper, terminalPos, AprsTerminalBlockEntity.class).isRadioOn(),
						"the terminal does not see the radio along the data wire"))
				.thenIdle(25)
				.thenExecute(() -> {
					AprsTerminalBlockEntity terminal = GameTestUtil.blockEntity(helper, terminalPos, AprsTerminalBlockEntity.class);
					helper.assertFalse(terminal.isStationFed(), "a data wire fed it power");
					helper.assertFalse(terminal.isOn(), "the terminal lit with no power of its own");
					GameTestUtil.placeWire(helper, terminalPos.east(), RadiocraftBlocks.WIRE.get());
					helper.setBlock(terminalBattery, RadiocraftBlocks.LARGE_BATTERY.get());
				})
				.thenIdle(2)
				.thenExecute(() -> charge(helper, terminalBattery))
				.thenWaitUntil(() -> helper.assertTrue(GameTestUtil.blockEntity(helper, terminalPos, AprsTerminalBlockEntity.class).isOn(),
						"the terminal stays dark with its own battery on the wire"))
				.thenSucceed();
	}
}
