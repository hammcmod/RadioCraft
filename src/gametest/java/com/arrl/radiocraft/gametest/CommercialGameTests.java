package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
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
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The commercial repeater and the commercial radio module: four-tall stacks that run on their own. */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CommercialGameTests {

	private static void stack(GameTestHelper helper, BlockPos bottom, BlockState state) {
		for(int i = 0; i < StackedBlocks.HEIGHT; i++)
			helper.setBlock(bottom.above(i), state.setValue(StackedBlocks.PART, i));
	}

	/** The repeater runs with no power wire and no duplexer block, its lamps light, and it comes down as one. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 60)
	public static void commercialRepeaterRunsOnItsOwn(GameTestHelper helper) {
		BlockPos bottom = new BlockPos(4, 2, 4);
		stack(helper, bottom, RadiocraftBlocks.COMMERCIAL_REPEATER.get().defaultBlockState());
		helper.runAfterDelay(2, () -> {
			CommercialRepeaterBlockEntity repeater = GameTestUtil.blockEntity(helper, bottom, CommercialRepeaterBlockEntity.class);
			RadioNetworkObject network = GameTestUtil.networkObject(helper, bottom, RadioNetworkObject.class);
			if(!network.isPowered)
				repeater.toggle();
		});
		helper.runAfterDelay(10, () -> {
			CommercialRepeaterBlockEntity repeater = GameTestUtil.blockEntity(helper, bottom, CommercialRepeaterBlockEntity.class);
			helper.assertTrue(repeater.isPowered(), "it should run with no battery on a wire");
			helper.assertTrue(repeater.hasDuplexer(), "its duplexer is built in");
			helper.assertTrue(helper.getBlockState(bottom.above(1)).getValue(CommercialRepeaterBlock.LAMP_PWR), "the power lamp is out");
			helper.setBlock(bottom.above(2), Blocks.AIR);
			for(int i = 0; i < StackedBlocks.HEIGHT; i++)
				helper.assertBlockNotPresent(RadiocraftBlocks.COMMERCIAL_REPEATER.get(), bottom.above(i));
			helper.succeed();
		});
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
