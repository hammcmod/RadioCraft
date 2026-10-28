package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.CommercialRadioModuleBlockEntity;
import com.arrl.radiocraft.common.blockentities.CommercialRepeaterBlockEntity;
import com.arrl.radiocraft.common.blocks.CommercialRadioModuleBlock;
import com.arrl.radiocraft.common.blocks.StackedBlocks;
import com.arrl.radiocraft.common.blocks.radios.CommercialRepeaterBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.TransmitterLog;
import com.arrl.radiocraft.common.radio.voice.StationProgram;
import net.minecraft.core.BlockPos;
import com.arrl.radiocraft.RadiocraftServerConfig;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The commercial repeater and the commercial radio module: four-tall racks (the module runs on its own, the repeater off the station's wire). */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CommercialGameTests {

	private static final String POWER_BATCH = "power_commercial_repeater";

	@BeforeBatch(batch = POWER_BATCH)
	public static void powerRequired(ServerLevel level) {
		RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(true);
	}

	@AfterBatch(batch = POWER_BATCH)
	public static void powerFree(ServerLevel level) {
		RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(false);
	}

	private static void stack(GameTestHelper helper, BlockPos bottom, BlockState state) {
		for(int i = 0; i < StackedBlocks.HEIGHT; i++)
			helper.setBlock(bottom.above(i), state.setValue(StackedBlocks.PART, i));
	}

	/**
	 * The repeater wants the station's power wire: dark without one, running (with its built-in duplexer, lamps lit)
	 * once a charged battery is wired to any cell of the rack, here the top one. It comes down as one. Power draw is
	 * off for the GameTest server as a whole ({@link GameTestDefaults}): on for this batch.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = POWER_BATCH)
	public static void commercialRepeaterRunsOffAWireToAnyCell(GameTestHelper helper) {
		BlockPos bottom = new BlockPos(4, 2, 4);
		BlockPos wire = bottom.above(3).north(), battery = wire.north();
		stack(helper, bottom, RadiocraftBlocks.COMMERCIAL_REPEATER.get().defaultBlockState());
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					CommercialRepeaterBlockEntity repeater = GameTestUtil.blockEntity(helper, bottom, CommercialRepeaterBlockEntity.class);
					if(!GameTestUtil.networkObject(helper, bottom, RadioNetworkObject.class).wantsOn)
						repeater.toggle();
				})
				.thenIdle(5)
				.thenExecute(() -> {
					CommercialRepeaterBlockEntity repeater = GameTestUtil.blockEntity(helper, bottom, CommercialRepeaterBlockEntity.class);
					helper.assertFalse(repeater.isPowered(), "it runs with no power wire");
					helper.assertFalse(helper.getBlockState(bottom.above(1)).getValue(CommercialRepeaterBlock.LAMP_PWR), "the power lamp is lit with no power");
					helper.setBlock(battery, RadiocraftBlocks.LARGE_BATTERY.get());
					GameTestUtil.placeWire(helper, wire, RadiocraftBlocks.WIRE.get());
				})
				.thenIdle(2)
				.thenExecute(() -> {
					GameTestUtil.networkObject(helper, battery, BatteryNetworkObject.class).getStorage().setEnergy(100_000);
					helper.assertTrue(GameTestUtil.networkObject(helper, bottom, RadioNetworkObject.class).hasBattery(), "the wire to the top cell does not reach the works in the bottom one");
				})
				.thenWaitUntil(() -> helper.assertTrue(GameTestUtil.blockEntity(helper, bottom, CommercialRepeaterBlockEntity.class).isPowered(), "it does not run off the battery"))
				.thenExecute(() -> {
					CommercialRepeaterBlockEntity repeater = GameTestUtil.blockEntity(helper, bottom, CommercialRepeaterBlockEntity.class);
					helper.assertTrue(repeater.hasDuplexer(), "its duplexer is built in");
					helper.assertTrue(helper.getBlockState(bottom.above(1)).getValue(CommercialRepeaterBlock.LAMP_PWR), "the power lamp is out");
					helper.setBlock(bottom.above(2), Blocks.AIR);
					for(int i = 0; i < StackedBlocks.HEIGHT; i++)
						helper.assertBlockNotPresent(RadiocraftBlocks.COMMERCIAL_REPEATER.get(), bottom.above(i));
				})
				.thenSucceed();
	}

	/**
	 * Wired straight to a solar panel at noon, with no charge controller, the repeater runs and fills its own battery;
	 * with the panel gone it keeps running on that battery (beacon blinking) and only goes dark once it is flat.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = POWER_BATCH)
	public static void commercialRepeaterChargesFromSolarAndRidesOutAnOutage(GameTestHelper helper) {
		BlockPos bottom = new BlockPos(4, 2, 4);
		BlockPos wire = bottom.above(1).north(), panel = wire.north();
		helper.getLevel().setDayTime(6000); // Noon.
		stack(helper, bottom, RadiocraftBlocks.COMMERCIAL_REPEATER.get().defaultBlockState());
		helper.setBlock(panel, RadiocraftBlocks.SOLAR_PANEL.get());
		GameTestUtil.placeWire(helper, wire, RadiocraftBlocks.WIRE.get());
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					CommercialRepeaterBlockEntity repeater = GameTestUtil.blockEntity(helper, bottom, CommercialRepeaterBlockEntity.class);
					if(!GameTestUtil.networkObject(helper, bottom, RadioNetworkObject.class).wantsOn)
						repeater.toggle();
				})
				.thenWaitUntil(() -> helper.assertTrue(GameTestUtil.blockEntity(helper, bottom, CommercialRepeaterBlockEntity.class).isPowered(), "a solar panel on its wire does not run it"))
				.thenExecute(() -> helper.assertFalse(GameTestUtil.networkObject(helper, bottom, CommercialRepeaterBlockEntity.Commercial.class).isOnBattery(), "it says it is on battery with the sun on the panel"))
				.thenWaitUntil(() -> helper.assertTrue(GameTestUtil.networkObject(helper, bottom, CommercialRepeaterBlockEntity.Commercial.class).getStorage().getEnergyStored() >= 400,
						"its battery is not filling from the panel"))
				.thenExecute(() -> helper.setBlock(panel, Blocks.AIR))
				.thenIdle(40)
				.thenExecute(() -> {
					CommercialRepeaterBlockEntity.Commercial works = GameTestUtil.networkObject(helper, bottom, CommercialRepeaterBlockEntity.Commercial.class);
					helper.assertTrue(works.isPowered, "it went dark the moment the panel was gone");
					helper.assertTrue(works.isOnBattery(), "it does not know it is running on its battery");
					works.getStorage().setEnergy(0);
				})
				.thenIdle(3)
				.thenExecute(() -> helper.assertFalse(GameTestUtil.blockEntity(helper, bottom, CommercialRepeaterBlockEntity.class).isPowered(), "it runs on with a flat battery and no panel"))
				.thenSucceed();
	}

	/** A module set to beep for direction finders goes on the air by itself, where it was told, and lights its TX lamp. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100)
	public static void moduleBeepsForDirectionFinders(GameTestHelper helper) {
		BlockPos bottom = new BlockPos(8, 2, 8);
		stack(helper, bottom, RadiocraftBlocks.COMMERCIAL_RADIO_MODULE.get().defaultBlockState());
		float hz = 146_520_000.0F;
		helper.runAfterDelay(1, () -> GameTestUtil.blockEntity(helper, bottom, CommercialRadioModuleBlockEntity.class)
				.configure(StationProgram.Kind.DF_BEEPER, Band.getBand("2m"), hz));
		helper.succeedWhen(() -> {
			helper.assertTrue(TransmitterLog.nearest(helper.getLevel(), helper.absolutePos(bottom), hz, 1000.0D, 64.0D, 20) != null,
					"nothing on " + hz + " Hz from the module");
			helper.assertTrue(helper.getBlockState(bottom.above(2)).getValue(CommercialRadioModuleBlock.LAMP_TX), "the TX lamp is out");
		});
	}

	/** A VHF base station a dozen blocks off, tuned to the module, hears its beeper (as it would a handheld). */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 120)
	public static void vhfRadioHearsTheModule(GameTestHelper helper) {
		BlockPos bottom = new BlockPos(2, 2, 2), radio = new BlockPos(13, 2, 12);
		stack(helper, bottom, RadiocraftBlocks.COMMERCIAL_RADIO_MODULE.get().defaultBlockState());
		GameTestUtil.radioWithJPole(helper, radio);
		float hz = 144_200_000.0F;
		helper.runAfterDelay(2, () -> {
			GameTestUtil.blockEntity(helper, bottom, CommercialRadioModuleBlockEntity.class)
					.configure(StationProgram.Kind.DF_BEEPER, Band.getBand("2m"), hz);
			com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity station =
					GameTestUtil.blockEntity(helper, radio, com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity.class);
			station.setFrequency(hz);
			if(!GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).isPowered)
				station.toggle();
		});
		helper.succeedWhen(() -> {
			com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity station =
					GameTestUtil.blockEntity(helper, radio, com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity.class);
			helper.assertTrue(station.getLastSignalGameTime() >= 0, "the base station never heard the module");
		});
	}

	/** Left unset, a module picks its own program and frequency; broken, the whole cabinet goes. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
	public static void moduleSetsItselfUp(GameTestHelper helper) {
		BlockPos bottom = new BlockPos(4, 2, 10);
		stack(helper, bottom, RadiocraftBlocks.COMMERCIAL_RADIO_MODULE.get().defaultBlockState());
		helper.runAfterDelay(5, () -> {
			CommercialRadioModuleBlockEntity module = GameTestUtil.blockEntity(helper, bottom, CommercialRadioModuleBlockEntity.class);
			helper.assertTrue(module.getKind() != null, "it picked no program");
			Band band = Band.getBand(module.getBandName());
			helper.assertTrue(band != null && module.getFrequency() >= band.minFrequency() && module.getFrequency() <= band.maxFrequency(),
					"its frequency " + module.getFrequency() + " is outside " + module.getBandName());
			helper.getLevel().setBlock(helper.absolutePos(bottom), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			for(int i = 0; i < StackedBlocks.HEIGHT; i++)
				helper.assertBlockNotPresent(RadiocraftBlocks.COMMERCIAL_RADIO_MODULE.get(), bottom.above(i));
			helper.succeed();
		});
	}
}
