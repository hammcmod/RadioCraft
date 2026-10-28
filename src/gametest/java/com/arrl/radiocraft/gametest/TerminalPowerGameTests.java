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
 * The H19 terminal is a mains-fed box: dark without power on its wire, and dark without a powered radio reachable
 * through the station gear. The GameTest server runs with power draw off ({@link GameTestDefaults}); on for this batch.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class TerminalPowerGameTests {

	private static final String BATCH = "power_terminal";
	private static final BlockPos RADIO = new BlockPos(3, 2, 6), IFACE = RADIO.north(), TERMINAL = IFACE.north();
	private static final BlockPos RADIO_BATTERY = RADIO.east(2), TERMINAL_BATTERY = TERMINAL.east(2);

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

	/** Radio, TNC, H19 in a row; the radio on its own charged battery. The terminal only lights once its own wire has power too. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 120, batch = BATCH)
	public static void terminalNeedsItsOwnPowerAndAPoweredRadio(GameTestHelper helper) {
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
				.thenIdle(25)
				.thenExecute(() -> {
					helper.assertTrue(terminal(helper).isRadioOn(), "the terminal does not see the powered radio through the TNC");
					helper.assertFalse(terminal(helper).isOn(), "the terminal lit with no power on its own wire");
					helper.assertTrue(terminal(helper).getDisplay() == Display.OFF, "its screen shows " + terminal(helper).getDisplay());
					GameTestUtil.placeWire(helper, TERMINAL.east(), RadiocraftBlocks.WIRE.get());
					helper.setBlock(TERMINAL_BATTERY, RadiocraftBlocks.LARGE_BATTERY.get());
				})
				.thenIdle(2)
				.thenExecute(() -> charge(helper, TERMINAL_BATTERY))
				.thenWaitUntil(() -> helper.assertTrue(terminal(helper).isOn(), "the terminal stays dark with its own battery on the wire"))
				.thenExecute(() -> {
					helper.assertTrue(terminal(helper).isBootPending(), "coming on did not queue a boot");
					GameTestUtil.networkObject(helper, RADIO_BATTERY, BatteryNetworkObject.class).getStorage().setEnergy(0); // The radio browns out...
				})
				.thenWaitUntil(() -> helper.assertFalse(terminal(helper).isOn(), "...and the terminal should go dark with no powered radio to talk to"))
				.thenSucceed();
	}
}
