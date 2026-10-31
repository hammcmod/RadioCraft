package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.IonosphericProcessorBlockEntity;
import com.arrl.radiocraft.common.blockentities.TeletypeBlockEntity;
import com.arrl.radiocraft.common.blockentities.WefaxReceiverBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.blocks.IonosphericProcessorBlock;
import com.arrl.radiocraft.common.blocks.antennas.DoubleVHFAntennaBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Ionospheric Processor reads the sky through an Ionosonde Antenna on its coax. A WEFAX or teletype beside it reads
 * it directly; one far away asks over the air (a QUERY) and prints the bulletin it answers with. With blackout alerts on
 * it warns of a forecast blackout, and a WEFAX that hears the alert prints it.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class IonosphericProcessorGameTests {

	private static final String BATCH = "ionospheric";

	/** A bulletin's fax tones take {@code wefax_bulletin_seconds}; two is enough to see them go out and keeps the tests short. */
	@net.minecraft.gametest.framework.BeforeBatch(batch = BATCH)
	public static void quickBulletins(net.minecraft.server.level.ServerLevel level) {
		RadiocraftServerConfig.WEFAX_BULLETIN_SECONDS.set(2);
		GameTestDefaults.calmSky(level); // Its stations talk over HF: a blackout left by another batch would silence them.
	}

	@net.minecraft.gametest.framework.AfterBatch(batch = BATCH)
	public static void normalBulletins(net.minecraft.server.level.ServerLevel level) {
		RadiocraftServerConfig.WEFAX_BULLETIN_SECONDS.set(RadiocraftServerConfig.WEFAX_BULLETIN_SECONDS.getDefault());
	}

	/** The processor at {@code lower} (two tall), coax west of it, and {@code antenna} (two tall) west of that. */
	private static void processor(GameTestHelper helper, BlockPos lower, Block antenna) {
		helper.setBlock(lower, RadiocraftBlocks.IONOSPHERIC_PROCESSOR.get().defaultBlockState()
				.setValue(IonosphericProcessorBlock.HALF, DoubleBlockHalf.LOWER).setValue(IonosphericProcessorBlock.FACING, Direction.SOUTH));
		helper.setBlock(lower.above(), RadiocraftBlocks.IONOSPHERIC_PROCESSOR.get().defaultBlockState()
				.setValue(IonosphericProcessorBlock.HALF, DoubleBlockHalf.UPPER).setValue(IonosphericProcessorBlock.FACING, Direction.SOUTH));
		GameTestUtil.placeWire(helper, lower.west(), RadiocraftBlocks.COAX_WIRE.get());
		BlockPos mast = lower.west(2);
		helper.setBlock(mast, antenna.defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.LOWER));
		helper.setBlock(mast.above(), antenna.defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.UPPER));
	}

	private static IonosphericProcessorBlockEntity processorAt(GameTestHelper helper, BlockPos lower) {
		return GameTestUtil.blockEntity(helper, lower, IonosphericProcessorBlockEntity.class);
	}

	private static WefaxReceiverBlockEntity wefaxAt(GameTestHelper helper, BlockPos pos) {
		return GameTestUtil.blockEntity(helper, pos, WefaxReceiverBlockEntity.class);
	}

	private static RadioBlockEntity radio(GameTestHelper helper, BlockPos pos) {
		return GameTestUtil.blockEntity(helper, pos, RadioBlockEntity.class);
	}

	private static void powerOn(GameTestHelper helper, BlockPos pos) {
		if(!GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class).isPowered)
			radio(helper, pos).toggle();
	}

	/**
	 * The processor with a VHF radio east of it, and a second radio {@code apart} further east (bare connectors: data
	 * packets only decode close in), both on {@code hz}.
	 */
	private static void airLink(GameTestHelper helper, BlockPos lower, int apart, float hz) {
		processor(helper, lower, RadiocraftBlocks.IONOSONDE.get());
		helper.setBlock(lower.east(), RadiocraftBlocks.VHF_BASE_STATION.get());
		helper.setBlock(lower.east(1 + apart), RadiocraftBlocks.VHF_BASE_STATION.get());
		radio(helper, lower.east()).setFrequency(hz);
		radio(helper, lower.east(1 + apart)).setFrequency(hz);
	}

	/** A WEFAX touching the processor reads it straight off with SOLR WX, no radio needed. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
	public static void wefaxBesideItReadsItDirectly(GameTestHelper helper) {
		BlockPos lower = new BlockPos(4, 2, 4);
		processor(helper, lower, RadiocraftBlocks.IONOSONDE.get());
		BlockPos wefax = lower.east();
		helper.setBlock(wefax, RadiocraftBlocks.WEFAX_RECEIVER.get());
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(processorAt(helper, lower).isOnline(), "the processor does not see its ionosonde"))
				.thenExecute(() -> {
					WefaxReceiverBlockEntity receiver = wefaxAt(helper, wefax);
					receiver.insertPaper(new ItemStack(Items.PAPER, 2));
					helper.assertTrue(receiver.cannotPrint(true) == null, "SOLR WX refused: " + receiver.cannotPrint(true));
					receiver.printSolar(null);
					helper.assertTrue(receiver.isPrinting() && !receiver.isRequesting(), "SOLR WX did not print the processor's report at once");
				})
				.thenSucceed();
	}

	/** Without an ionosonde (a J-pole on its coax instead) it is offline, and the WEFAX beside it says so. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 120, batch = BATCH)
	public static void needsItsIonosonde(GameTestHelper helper) {
		BlockPos lower = new BlockPos(4, 2, 4);
		processor(helper, lower, RadiocraftBlocks.J_POLE_ANTENNA.get());
		BlockPos wefax = lower.east();
		helper.setBlock(wefax, RadiocraftBlocks.WEFAX_RECEIVER.get());
		helper.runAtTickTime(100, () -> {
			helper.assertFalse(processorAt(helper, lower).isOnline(), "online with a J-pole instead of an ionosonde");
			helper.assertTrue(processorAt(helper, lower).getConditionLevel() == 0, "a comparator reads it offline");
			wefaxAt(helper, wefax).insertPaper(new ItemStack(Items.PAPER));
			helper.assertTrue("screen.radiocraft.wefax.processor_offline".equals(wefaxAt(helper, wefax).cannotPrint(true)),
					"SOLR WX gave " + wefaxAt(helper, wefax).cannotPrint(true));
			helper.succeed();
		});
	}

	/** SOLR WX on a far WEFAX asks over the air; the processor answers and the WEFAX prints its bulletin. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 500, batch = BATCH)
	public static void wefaxAsksOverTheAir(GameTestHelper helper) {
		BlockPos lower = new BlockPos(3, 2, 4);
		airLink(helper, lower, 3, 145_810_000.0F);
		BlockPos far = lower.east(4);
		BlockPos wefax = far.north();
		helper.setBlock(wefax, RadiocraftBlocks.WEFAX_RECEIVER.get());
		helper.startSequence()
				.thenExecute(() -> {
					powerOn(helper, lower.east());
					powerOn(helper, far);
				})
				.thenWaitUntil(() -> helper.assertTrue(processorAt(helper, lower).isOnline() && radio(helper, far).isPowered(), "not up yet"))
				.thenExecute(() -> {
					WefaxReceiverBlockEntity receiver = wefaxAt(helper, wefax);
					receiver.insertPaper(new ItemStack(Items.PAPER, 2));
					helper.assertTrue(receiver.localProcessor() == null, "the far WEFAX found the processor as if beside it");
					receiver.printSolar(null);
					helper.assertTrue(receiver.isRequesting(), "SOLR WX did not ask over the air");
				})
				.thenWaitUntil(() -> helper.assertTrue(wefaxAt(helper, wefax).isPrinting(), "no answer printed; requesting="
						+ wefaxAt(helper, wefax).isRequesting() + " processor sent " + processorAt(helper, lower).getBulletinsSent()))
				.thenExecute(() -> helper.assertTrue(wefaxAt(helper, wefax).getSolarFrom().equals(processorAt(helper, lower).sender()),
						"the answer came from " + wefaxAt(helper, wefax).getSolarFrom()))
				.thenSucceed();
	}

	/** SOLAR WX on a far teletype asks over the air too, and prints the report the processor answers with. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 500, batch = BATCH)
	public static void teletypeAsksOverTheAir(GameTestHelper helper) {
		BlockPos lower = new BlockPos(3, 2, 10);
		airLink(helper, lower, 3, 145_850_000.0F);
		BlockPos far = lower.east(4);
		BlockPos teletypePos = far.north();
		helper.setBlock(teletypePos, RadiocraftBlocks.TELETYPE.get());
		helper.startSequence()
				.thenExecute(() -> {
					powerOn(helper, lower.east());
					powerOn(helper, far);
					GameTestUtil.blockEntity(helper, teletypePos, TeletypeBlockEntity.class).insertPaper(new ItemStack(Items.PAPER, 4));
				})
				.thenWaitUntil(() -> helper.assertTrue(processorAt(helper, lower).isOnline() && radio(helper, far).isPowered(), "not up yet"))
				.thenExecute(() -> {
					TeletypeBlockEntity teletype = GameTestUtil.blockEntity(helper, teletypePos, TeletypeBlockEntity.class);
					teletype.printReport();
					helper.assertTrue(teletype.isRequesting(), "SOLAR WX did not ask over the air: " + teletype.getRttyLines());
				})
				.thenWaitUntil(() -> {
					TeletypeBlockEntity teletype = GameTestUtil.blockEntity(helper, teletypePos, TeletypeBlockEntity.class);
					helper.assertTrue(!teletype.isRequesting() && teletype.getRttyLines().size() > 3, "no report printed: " + teletype.getRttyLines());
				})
				.thenExecute(() -> helper.assertFalse(GameTestUtil.blockEntity(helper, teletypePos, TeletypeBlockEntity.class).getRttyLines().contains("NO ANSWER"), "it timed out"))
				.thenSucceed();
	}

	/**
	 * A blackout is forecast: a processor with alerts on warns over the air and the far WEFAX prints it; one with alerts
	 * switched off says nothing to the WEFAX beside it. Its own batch: the forecast is the whole dimension's.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400, batch = BATCH + "_alert")
	public static void blackoutAlertPrintsAtTheFarWefax(GameTestHelper helper) {
		BlockPos lower = new BlockPos(3, 2, 4);
		airLink(helper, lower, 3, 145_870_000.0F);
		BlockPos far = lower.east(4);
		BlockPos wefax = far.north();
		helper.setBlock(wefax, RadiocraftBlocks.WEFAX_RECEIVER.get());
		BlockPos quiet = new BlockPos(3, 2, 12);
		processor(helper, quiet, RadiocraftBlocks.IONOSONDE.get());
		BlockPos quietWefax = quiet.east();
		helper.setBlock(quietWefax, RadiocraftBlocks.WEFAX_RECEIVER.get());
		helper.startSequence()
				.thenExecute(() -> {
					powerOn(helper, lower.east());
					powerOn(helper, far);
					wefaxAt(helper, wefax).insertPaper(new ItemStack(Items.PAPER, 2));
					wefaxAt(helper, quietWefax).insertPaper(new ItemStack(Items.PAPER, 2));
					processorAt(helper, quiet).setBlackoutAlerts(false);
				})
				.thenWaitUntil(() -> helper.assertTrue(processorAt(helper, lower).isOnline() && processorAt(helper, quiet).isOnline() && radio(helper, far).isPowered(), "not up yet"))
				.thenExecute(() -> SolarEventManager.forecast(helper.getLevel(), RadiocraftData.SOLAR_EVENTS.getValue(Radiocraft.id("blackout"))))
				.thenWaitUntil(() -> helper.assertTrue(wefaxAt(helper, wefax).hasHeardAlert(), "the far WEFAX heard no alert; processor sent " + processorAt(helper, lower).getAlertsSent()))
				.thenExecute(() -> {
					helper.assertTrue(wefaxAt(helper, wefax).isPrinting(), "the alert did not print");
					helper.assertTrue(processorAt(helper, lower).getBlackoutMinutes() >= 0, "the processor's screen shows no forecast");
					helper.assertFalse(wefaxAt(helper, quietWefax).hasHeardAlert(), "a processor with alerts off sent one");
					SolarEventManager.clearForecast(helper.getLevel());
				})
				.thenSucceed();
	}

	/** With timed bulletins on, a bulletin goes out as soon as a radio is on, unasked. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400, batch = BATCH)
	public static void timedBulletinsGoOutUnasked(GameTestHelper helper) {
		BlockPos lower = new BlockPos(3, 2, 7);
		airLink(helper, lower, 3, 145_890_000.0F);
		BlockPos far = lower.east(4);
		BlockPos wefax = far.south();
		helper.setBlock(wefax, RadiocraftBlocks.WEFAX_RECEIVER.get());
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(processorAt(helper, lower).isOnline(), "the processor does not see its ionosonde"))
				.thenExecute(() -> processorAt(helper, lower).setTimedBulletins(true))
				.thenIdle(25) // Its first bulletin finds no radio on.
				.thenExecute(() -> {
					helper.assertFalse(wefaxAt(helper, wefax).hasSolarBulletin(), "the far WEFAX heard a bulletin with the radios off");
					powerOn(helper, lower.east());
					powerOn(helper, far);
				})
				.thenWaitUntil(() -> helper.assertTrue(wefaxAt(helper, wefax).hasSolarBulletin(), "the far WEFAX never heard the bulletin"))
				.thenSucceed();
	}

	/**
	 * Over the air the WEFAX starts printing as the bulletin's start tone ends, not after the whole sheet has been sent:
	 * the sending radio shows it transmitting all the while, and lines come in one after another.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
	public static void bulletinPrintsAsItComesIn(GameTestHelper helper) {
		BlockPos lower = new BlockPos(3, 2, 13);
		airLink(helper, lower, 3, 145_930_000.0F);
		BlockPos sender = lower.east(), far = lower.east(4);
		BlockPos wefax = far.north();
		helper.setBlock(wefax, RadiocraftBlocks.WEFAX_RECEIVER.get());
		helper.startSequence()
				.thenExecute(() -> {
					powerOn(helper, sender);
					powerOn(helper, far);
					wefaxAt(helper, wefax).insertPaper(new ItemStack(Items.PAPER, 2));
				})
				.thenWaitUntil(() -> helper.assertTrue(processorAt(helper, lower).isOnline() && radio(helper, far).isPowered(), "not up yet"))
				.thenExecute(() -> wefaxAt(helper, wefax).printSolar(null))
				.thenWaitUntil(() -> helper.assertTrue(wefaxAt(helper, wefax).isPrinting(), "no answer printing"))
				.thenExecute(() -> {
					helper.assertTrue(radio(helper, sender).isSendingData() && radio(helper, sender).isSendingDataSynced(),
							"the processor's radio is not showing it transmitting while the sheet comes in");
					helper.assertTrue(wefaxAt(helper, wefax).progress(helper.getLevel().getGameTime()) < 0.5F, "the sheet arrived all at once");
				})
				.thenWaitUntil(() -> helper.assertTrue(wefaxAt(helper, wefax).hasReadySheet(), "the sheet never finished"))
				.thenExecute(() -> helper.assertFalse(wefaxAt(helper, wefax).hasLostLines(), "a clean signal printed speckled lines"))
				.thenSucceed();
	}

	/** The sending radio goes off part way through: the rest of the sheet prints as speckle, and it still comes out. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
	public static void lostSignalPrintsSpeckle(GameTestHelper helper) {
		BlockPos lower = new BlockPos(3, 2, 16);
		airLink(helper, lower, 3, 145_970_000.0F);
		BlockPos sender = lower.east(), far = lower.east(4);
		BlockPos wefax = far.north();
		helper.setBlock(wefax, RadiocraftBlocks.WEFAX_RECEIVER.get());
		helper.startSequence()
				.thenExecute(() -> {
					powerOn(helper, sender);
					powerOn(helper, far);
					wefaxAt(helper, wefax).insertPaper(new ItemStack(Items.PAPER, 2));
				})
				.thenWaitUntil(() -> helper.assertTrue(processorAt(helper, lower).isOnline() && radio(helper, far).isPowered(), "not up yet"))
				.thenExecute(() -> wefaxAt(helper, wefax).printSolar(null))
				.thenWaitUntil(() -> helper.assertTrue(wefaxAt(helper, wefax).isPrinting(), "no answer printing"))
				.thenIdle(5)
				.thenExecute(() -> radio(helper, sender).toggle()) // Off the air part way through.
				.thenWaitUntil(() -> helper.assertTrue(wefaxAt(helper, wefax).hasReadySheet(), "the sheet never finished"))
				.thenExecute(() -> helper.assertTrue(wefaxAt(helper, wefax).hasLostLines(), "lines after the signal went printed clean"))
				.thenSucceed();
	}

	private static final String BLACKOUT_BATCH = BATCH + "_blackout";

	@net.minecraft.gametest.framework.BeforeBatch(batch = BLACKOUT_BATCH)
	public static void blackout(net.minecraft.server.level.ServerLevel level) {
		RadiocraftServerConfig.WEFAX_BULLETIN_SECONDS.set(2);
		SolarEventManager.startEvent(level, RadiocraftData.SOLAR_EVENTS.getValue(Radiocraft.id("blackout")));
	}

	@net.minecraft.gametest.framework.AfterBatch(batch = BLACKOUT_BATCH)
	public static void afterBlackout(net.minecraft.server.level.ServerLevel level) {
		RadiocraftServerConfig.WEFAX_BULLETIN_SECONDS.set(RadiocraftServerConfig.WEFAX_BULLETIN_SECONDS.getDefault());
		GameTestDefaults.calmSky(level);
	}

	/**
	 * In a blackout HF skip is dead, but stations close by still hear each other at half range (maintainer,
	 * 2026-10-31): two 40m radios a block apart on bare connectors still get a solar bulletin across. (A bare
	 * connector loses signal at both ends, so it reaches about a block in a blackout; real antennas reach far further.)
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BLACKOUT_BATCH)
	public static void blackoutStillCarriesLocally(GameTestHelper helper) {
		BlockPos lower = new BlockPos(3, 2, 4);
		processor(helper, lower, RadiocraftBlocks.IONOSONDE.get());
		BlockPos sender = lower.east(), far = lower.east(3);
		helper.setBlock(sender, RadiocraftBlocks.HF_RADIO_40M.get());
		helper.setBlock(far, RadiocraftBlocks.HF_RADIO_40M.get());
		BlockPos wefax = far.north();
		helper.setBlock(wefax, RadiocraftBlocks.WEFAX_RECEIVER.get());
		helper.startSequence()
				.thenExecute(() -> {
					radio(helper, sender).setFrequency(7_050_000.0F);
					radio(helper, far).setFrequency(7_050_000.0F);
					powerOn(helper, sender);
					powerOn(helper, far);
					wefaxAt(helper, wefax).insertPaper(new ItemStack(Items.PAPER, 2));
				})
				.thenWaitUntil(() -> helper.assertTrue(processorAt(helper, lower).isOnline() && radio(helper, far).isPowered(), "not up yet"))
				.thenExecute(() -> {
					helper.assertTrue(radio(helper, far).getNoiseFloor() >= 0.99D, "no blackout: noise " + radio(helper, far).getNoiseFloor());
					wefaxAt(helper, wefax).printSolar(null);
					helper.assertTrue(wefaxAt(helper, wefax).isRequesting(), "SOLR WX did not ask over the air");
				})
				.thenWaitUntil(() -> helper.assertTrue(wefaxAt(helper, wefax).isPrinting(), "nobody answered in the blackout"))
				.thenSucceed();
	}
}
