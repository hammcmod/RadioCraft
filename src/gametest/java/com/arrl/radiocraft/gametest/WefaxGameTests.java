package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blockentities.WefaxReceiverBlockEntity;
import com.arrl.radiocraft.common.blocks.WefaxReceiverBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The WEFAX receiver prints on paper, one sheet a chart; the sheet waits in the tray until taken, or is pushed out of the front. */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WefaxGameTests {

	/** The tests here set these for themselves; whatever happens, the next batch gets the defaults back. */
	@net.minecraft.gametest.framework.AfterBatch(batch = "wefax")
	public static void restoreConfig(net.minecraft.server.level.ServerLevel level) {
		RadiocraftServerConfig.TELETYPE_NEEDS_RADIO.set(RadiocraftServerConfig.TELETYPE_NEEDS_RADIO.getDefault());
		RadiocraftServerConfig.WEFAX_SOLAR_ALWAYS.set(RadiocraftServerConfig.WEFAX_SOLAR_ALWAYS.getDefault());
	}

	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400, batch = "wefax")
	public static void printsASheetOutOfTheFront(GameTestHelper helper) {
		boolean needsRadio = RadiocraftServerConfig.TELETYPE_NEEDS_RADIO.get();
		boolean solarAlways = RadiocraftServerConfig.WEFAX_SOLAR_ALWAYS.get();
		RadiocraftServerConfig.TELETYPE_NEEDS_RADIO.set(false); // Here it is about the paper, not the radio
		RadiocraftServerConfig.WEFAX_SOLAR_ALWAYS.set(true); // or the Ionospheric Processor.
		BlockPos pos = new BlockPos(6, 2, 6);
		helper.setBlock(pos, RadiocraftBlocks.WEFAX_RECEIVER.get().defaultBlockState().setValue(WefaxReceiverBlock.FACING, Direction.SOUTH));
		helper.startSequence()
				.thenExecute(() -> {
					WefaxReceiverBlockEntity receiver = GameTestUtil.blockEntity(helper, pos, WefaxReceiverBlockEntity.class);
					helper.assertTrue("screen.radiocraft.wefax.no_paper".equals(receiver.cannotPrint(true)), "it printed without paper");
					receiver.insertPaper(new ItemStack(Items.PAPER, 3));
					receiver.printSolar(null);
					helper.assertTrue(receiver.isPrinting(), "it is not printing");
					helper.assertTrue(receiver.getPaper() == 2, "a sheet should be used, " + receiver.getPaper() + " left");
					helper.assertTrue(receiver.getPixels().length == 128 * 128, "no picture to draw");
					helper.assertTrue(receiver.getSignal() == 1, "a message gives redstone 1, got " + receiver.getSignal());
					helper.assertTrue(helper.getBlockState(pos).getSignal(helper.getLevel(), helper.absolutePos(pos), Direction.NORTH) == 1,
							"the block does not put out the signal");
				})
				.thenWaitUntil(() -> helper.assertTrue(GameTestUtil.blockEntity(helper, pos, WefaxReceiverBlockEntity.class).hasReadySheet(), "no sheet in the tray yet"))
				.thenExecute(() -> {
					WefaxReceiverBlockEntity receiver = GameTestUtil.blockEntity(helper, pos, WefaxReceiverBlockEntity.class);
					helper.assertFalse(receiver.isPrinting(), "still printing");
					helper.assertTrue(helper.getBlockState(pos).getValue(WefaxReceiverBlock.READY), "the model does not show the sheet in the tray");
					helper.assertItemEntityNotPresent(Items.FILLED_MAP, pos.south(), 1.5D);
					receiver.takeSheet(null); // Nobody to hand it to: out of the front.
					helper.assertFalse(helper.getBlockState(pos).getValue(WefaxReceiverBlock.READY), "the tray still shows a sheet");
					helper.assertTrue(receiver.getSignal() == 0, "the signal stayed on after the sheet was taken: " + receiver.getSignal());
				})
				.thenWaitUntil(() -> helper.assertItemEntityPresent(Items.FILLED_MAP, pos.south(), 1.5D))
				.thenExecute(() -> {
					RadiocraftServerConfig.TELETYPE_NEEDS_RADIO.set(needsRadio);
					RadiocraftServerConfig.WEFAX_SOLAR_ALWAYS.set(solarAlways);
				})
				.thenSucceed();
	}

	/** Without paper nothing prints and there is no signal, whatever comes in. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40, batch = "wefax")
	public static void noPaperNoSignal(GameTestHelper helper) {
		boolean solarAlways = RadiocraftServerConfig.WEFAX_SOLAR_ALWAYS.get();
		RadiocraftServerConfig.WEFAX_SOLAR_ALWAYS.set(true);
		BlockPos pos = new BlockPos(6, 2, 6);
		helper.setBlock(pos, RadiocraftBlocks.WEFAX_RECEIVER.get().defaultBlockState().setValue(WefaxReceiverBlock.FACING, Direction.SOUTH));
		helper.startSequence()
				.thenExecute(() -> {
					WefaxReceiverBlockEntity receiver = GameTestUtil.blockEntity(helper, pos, WefaxReceiverBlockEntity.class);
					receiver.printSolar(null);
					receiver.printWeather();
					RadiocraftServerConfig.WEFAX_SOLAR_ALWAYS.set(solarAlways);
					helper.assertFalse(receiver.isPrinting(), "it printed without paper");
					helper.assertTrue(receiver.getSignal() == 0, "a signal without paper: " + receiver.getSignal());
				})
				.thenSucceed();
	}

	/** A radio, a TNC, an H-19, then the WEFAX: it receives through the station gear between it and the radio. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = "wefax")
	public static void receivesThroughTheStationGear(GameTestHelper helper) {
		boolean needsRadio = RadiocraftServerConfig.TELETYPE_NEEDS_RADIO.get();
		RadiocraftServerConfig.TELETYPE_NEEDS_RADIO.set(true);
		BlockPos radio = new BlockPos(2, 2, 3);
		GameTestUtil.radioWithJPole(helper, radio);
		helper.setBlock(radio.east(), RadiocraftBlocks.RADIO_INTERFACE.get());
		helper.setBlock(radio.east(2), RadiocraftBlocks.APRS_TERMINAL.get());
		BlockPos wefax = radio.east(3);
		helper.setBlock(wefax, RadiocraftBlocks.WEFAX_RECEIVER.get());
		helper.succeedWhen(() -> {
			boolean has = GameTestUtil.blockEntity(helper, wefax, WefaxReceiverBlockEntity.class).hasRadio();
			if(has)
				RadiocraftServerConfig.TELETYPE_NEEDS_RADIO.set(needsRadio);
			helper.assertTrue(has, "the WEFAX does not see the radio through the TNC and H-19");
		});
	}
}
