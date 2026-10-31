package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity;
import com.arrl.radiocraft.common.blockentities.TeletypeBlockEntity;
import com.arrl.radiocraft.common.blockentities.TelephoneBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.blocks.WireBundleBlock;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.BEVoiceReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Data wire (maintainer, 2026-10-30): it links radios to accessories, wired speakers and microwave stations that do not
 * touch, and power wire no longer does.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DataWireGameTests {

	/** A wall speaker on a run of power wire from the radio is not played any more. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100)
	public static void powerWireNoLongerReachesSpeakers(GameTestHelper helper) {
		BlockPos radio = new BlockPos(2, 1, 2);
		helper.setBlock(radio, RadiocraftBlocks.HF_RADIO_20M.get());
		helper.setBlock(radio.east(3), RadiocraftBlocks.WALL_SPEAKER.get());
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					GameTestUtil.placeWire(helper, radio.east(), RadiocraftBlocks.WIRE.get());
					GameTestUtil.placeWire(helper, radio.east(2), RadiocraftBlocks.WIRE.get());
				})
				.thenIdle(45) // Long enough for two speaker refreshes and the walk cache to lapse.
				.thenExecute(() -> {
					List<BEVoiceReceiver.WiredSpeaker> wired = GameTestUtil.blockEntity(helper, radio, RadioBlockEntity.class).getVoiceReceiver().getWiredSpeakers();
					helper.assertTrue(wired.isEmpty(), "Power wire still reached a speaker: " + wired);
				})
				.thenSucceed();
	}

	/** A teletype five blocks from its radio, joined by data wire, works through it. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100)
	public static void teletypeReachesItsRadioOverDataWire(GameTestHelper helper) {
		BlockPos radio = new BlockPos(1, 1, 2), teletype = new BlockPos(6, 1, 2);
		helper.setBlock(radio, RadiocraftBlocks.HF_RADIO_20M.get());
		helper.setBlock(teletype, RadiocraftBlocks.TELETYPE.get());
		for(int x = 2; x <= 5; x++)
			GameTestUtil.placeWire(helper, new BlockPos(x, 1, 2), RadiocraftBlocks.DATA_WIRE.get());
		helper.startSequence()
				.thenIdle(2)
				.thenWaitUntil(() -> {
					List<RadioBlockEntity> radios = GameTestUtil.blockEntity(helper, teletype, TeletypeBlockEntity.class).getRadios();
					helper.assertTrue(radios.size() == 1 && radios.getFirst().getBlockPos().equals(helper.absolutePos(radio)),
							"The teletype did not find its radio over data wire: " + radios);
				})
				.thenSucceed();
	}

	/** A telephone wired to a microwave radio with data wire laid in a bundle beside power wire is one of its stations. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100)
	public static void microwaveServesAStationOverDataWireInABundle(GameTestHelper helper) {
		BlockPos microwave = new BlockPos(1, 1, 3), phone = new BlockPos(6, 1, 3);
		helper.setBlock(microwave, RadiocraftBlocks.MICROWAVE_RADIO.get());
		helper.setBlock(phone, RadiocraftBlocks.TELEPHONE.get());
		for(int x = 2; x <= 5; x++)
			GameTestUtil.placeWire(helper, new BlockPos(x, 1, 3), RadiocraftBlocks.WIRE.get());
		for(int x = 2; x <= 5; x++) {
			helper.assertTrue(WireBundleBlock.canAdd(helper.getLevel(), helper.absolutePos(new BlockPos(x, 1, 3)), RadiocraftBlocks.DATA_WIRE.get()),
					"Data wire cannot go in with power wire");
			WireBundleBlock.add(helper.getLevel(), helper.absolutePos(new BlockPos(x, 1, 3)), RadiocraftBlocks.DATA_WIRE.get());
		}
		helper.startSequence()
				.thenIdle(2)
				.thenWaitUntil(() -> {
					MicrowaveRadioBlockEntity radio = GameTestUtil.blockEntity(helper, microwave, MicrowaveRadioBlockEntity.class);
					helper.assertTrue(radio.stations().stream().anyMatch(be -> be instanceof TelephoneBlockEntity),
							"The telephone on the data wire is not one of the microwave radio's stations: " + radio.stations());
					helper.assertTrue(MicrowaveRadioBlockEntity.servingStation(helper.getLevel(), helper.absolutePos(phone)).contains(radio),
							"The telephone does not see the microwave radio serving it");
				})
				.thenSucceed();
	}

	/**
	 * Dyed data wire links like plain data wire, and two colours run side by side stay apart: a red run reaches its
	 * teletype; a red run that changes to blue half way does not.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100)
	public static void dyedDataWireJoinsOnlyItsOwnColour(GameTestHelper helper) {
		WireBlock red = RadiocraftBlocks.COLOURED_DATA_WIRE.get(net.minecraft.world.item.DyeColor.RED).get();
		WireBlock blue = RadiocraftBlocks.COLOURED_DATA_WIRE.get(net.minecraft.world.item.DyeColor.BLUE).get();
		BlockPos radio = new BlockPos(1, 1, 1), teletype = new BlockPos(6, 1, 1);
		BlockPos radio2 = new BlockPos(1, 1, 4), teletype2 = new BlockPos(6, 1, 4);
		helper.setBlock(radio, RadiocraftBlocks.HF_RADIO_20M.get());
		helper.setBlock(teletype, RadiocraftBlocks.TELETYPE.get());
		helper.setBlock(radio2, RadiocraftBlocks.HF_RADIO_20M.get());
		helper.setBlock(teletype2, RadiocraftBlocks.TELETYPE.get());
		for(int x = 2; x <= 5; x++) {
			GameTestUtil.placeWire(helper, new BlockPos(x, 1, 1), red);
			GameTestUtil.placeWire(helper, new BlockPos(x, 1, 4), x <= 3 ? red : blue);
		}
		helper.startSequence()
				.thenIdle(2)
				.thenWaitUntil(() -> helper.assertTrue(GameTestUtil.blockEntity(helper, teletype, TeletypeBlockEntity.class).getRadios().size() == 1,
						"The teletype did not find its radio over red data wire"))
				.thenExecute(() -> helper.assertTrue(GameTestUtil.blockEntity(helper, teletype2, TeletypeBlockEntity.class).getRadios().isEmpty(),
						"Red data wire joined blue"))
				.thenSucceed();
	}
}
