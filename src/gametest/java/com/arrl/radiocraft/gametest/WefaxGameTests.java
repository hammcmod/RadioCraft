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

/** The WEFAX receiver prints on paper, one sheet a chart, and the sheet comes out of its front. */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WefaxGameTests {

	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400, batch = "wefax")
	public static void printsASheetOutOfTheFront(GameTestHelper helper) {
		boolean needsRadio = RadiocraftServerConfig.TELETYPE_NEEDS_RADIO.get();
		RadiocraftServerConfig.TELETYPE_NEEDS_RADIO.set(false); // Here it is about the paper, not the radio.
		BlockPos pos = new BlockPos(6, 2, 6);
		helper.setBlock(pos, RadiocraftBlocks.WEFAX_RECEIVER.get().defaultBlockState().setValue(WefaxReceiverBlock.FACING, Direction.SOUTH));
		helper.startSequence()
				.thenExecute(() -> {
					WefaxReceiverBlockEntity receiver = GameTestUtil.blockEntity(helper, pos, WefaxReceiverBlockEntity.class);
					helper.assertTrue("screen.radiocraft.wefax.no_paper".equals(receiver.cannotPrint(true)), "it printed without paper");
					receiver.insertPaper(new ItemStack(Items.PAPER, 3));
					receiver.printSolar();
					helper.assertTrue(receiver.isPrinting(), "it is not printing");
					helper.assertTrue(receiver.getPaper() == 2, "a sheet should be used, " + receiver.getPaper() + " left");
					helper.assertTrue(receiver.getPixels().length == 128 * 128, "no picture to draw");
				})
				.thenWaitUntil(() -> helper.assertItemEntityPresent(Items.FILLED_MAP, pos.south(), 1.5D))
				.thenExecute(() -> {
					helper.assertFalse(GameTestUtil.blockEntity(helper, pos, WefaxReceiverBlockEntity.class).isPrinting(), "still printing");
					RadiocraftServerConfig.TELETYPE_NEEDS_RADIO.set(needsRadio);
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
