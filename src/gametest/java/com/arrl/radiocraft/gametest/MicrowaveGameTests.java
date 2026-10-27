package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blockentities.AprsTerminalBlockEntity;
import com.arrl.radiocraft.common.blockentities.MicrowaveDishBlockEntity;
import com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity;
import com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity.Phase;
import com.arrl.radiocraft.common.blockentities.RadioFaxBlockEntity;
import com.arrl.radiocraft.common.blockentities.TelephoneBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.TransmitterLog;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * Microwave links: two radios with dishes facing each other find each other and link once confirmed, drop the link
 * when a dish turns away, carry APRS between H19s and calls between telephones; callsign and ID pairs are unique;
 * and every transmission is logged for the handheld direction finder.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class MicrowaveGameTests {

	private static final String BATCH = "microwave";
	private static final BlockPos A = new BlockPos(2, 1, 4), B = new BlockPos(13, 1, 4);
	/** Callsigns new to each run: the test world keeps the last run's claims. */
	private static final String RUN = Integer.toString((int)(System.nanoTime() & 0xFFFFF), 36).toUpperCase(java.util.Locale.ROOT);

	/** A microwave radio at {@code radio} with coax {@code step} along x to a dish pointing the same way. */
	private static void station(GameTestHelper helper, BlockPos radio, int step) {
		helper.setBlock(radio, RadiocraftBlocks.MICROWAVE_RADIO.get());
		GameTestUtil.placeWire(helper, radio.offset(step, 0, 0), RadiocraftBlocks.COAX_WIRE.get());
		helper.setBlock(radio.offset(2 * step, 0, 0), RadiocraftBlocks.SATELLITE_DISH.get());
	}

	private static MicrowaveRadioBlockEntity radio(GameTestHelper helper, BlockPos pos) {
		return GameTestUtil.blockEntity(helper, pos, MicrowaveRadioBlockEntity.class);
	}

	private static MicrowaveDishBlockEntity dish(GameTestHelper helper, BlockPos pos) {
		return GameTestUtil.blockEntity(helper, pos, MicrowaveDishBlockEntity.class);
	}

	/** Both stations up, named, A broadcasting and B searching; quick sweeps for the test. */
	private static void link(GameTestHelper helper, String callA, String callB) {
		RadiocraftServerConfig.MICROWAVE_SEARCH_SECONDS.set(1);
		dish(helper, A.east(2)).setAzimuth(90.0D);
		dish(helper, B.west(2)).setAzimuth(270.0D);
		MicrowaveRadioBlockEntity a = radio(helper, A), b = radio(helper, B);
		a.togglePower();
		b.togglePower();
		helper.assertTrue(a.setIdentity(callA, 0x1A2F), "A could not take its callsign");
		helper.assertTrue(b.setIdentity(callB, 0x00F1), "B could not take its callsign");
	}

	/**
	 * The large horn is a microwave antenna like the dish, with {@code microwave_horn_range_factor} times its reach: a
	 * horn on A's coax pointing east at B's dish links the same way. A batch of its own, as dishes hear widely.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH + "_horn")
	public static void hornLinksLikeADish(GameTestHelper helper) {
		helper.setBlock(A, RadiocraftBlocks.MICROWAVE_RADIO.get());
		GameTestUtil.placeWire(helper, A.east(), RadiocraftBlocks.COAX_WIRE.get());
		com.arrl.radiocraft.common.blocks.MicrowaveHornBlock horn = RadiocraftBlocks.MICROWAVE_HORN.get();
		net.minecraft.world.level.block.state.BlockState state = horn.defaultBlockState().setValue(com.arrl.radiocraft.common.blocks.MicrowaveHornBlock.FACING, net.minecraft.core.Direction.EAST);
		helper.setBlock(A.east(2), state);
		horn.setPlacedBy(helper.getLevel(), helper.absolutePos(A.east(2)), state, null, new net.minecraft.world.item.ItemStack(com.arrl.radiocraft.common.init.RadiocraftItems.MICROWAVE_HORN.get()));
		station(helper, B, -1);
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> {
					helper.assertTrue(dish(helper, A.east(2)).isHorn(), "the horn's block entity is not a horn");
					helper.assertTrue(dish(helper, A.east(2)).getRange() > dish(helper, B.west(2)).getRange(), "the horn reaches no further than a dish");
					link(helper, "MWH" + RUN, "MWI" + RUN);
				})
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, A).isPowered() && radio(helper, A).getDishInfo().size() == 1 && radio(helper, A).getDishInfo().getFirst().horn(),
						"A does not see its horn on the coax"))
				.thenExecute(() -> {
					helper.assertTrue(radio(helper, A).broadcast(), "A could not broadcast");
					helper.assertTrue(radio(helper, B).search(), "B could not search");
				})
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, B).getPhase() == Phase.FOUND, "B has not found A's horn: " + radio(helper, B).getPhase()))
				.thenExecute(() -> radio(helper, B).confirm())
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, A).isLinked(), "A is not linked through its horn"))
				.thenExecute(() -> { // Off again: test plots stay in the world, and a horn reaches the other batches' stations.
					radio(helper, A).togglePower();
					radio(helper, B).togglePower();
				})
				.thenSucceed();
	}

	/** Facing dishes: B finds A, asks, and once confirmed is linked; turning A's dish away loses the link. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
	public static void linkFoundConfirmedAndLost(GameTestHelper helper) {
		station(helper, A, 1);
		station(helper, B, -1);
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> link(helper, "MWA" + RUN, "MWB" + RUN))
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, A).isPowered() && radio(helper, B).isPowered(), "Radios did not power up"))
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, A).getDishInfo().size() == 1, "A does not see its dish on the coax"))
				.thenExecute(() -> {
					helper.assertTrue(radio(helper, A).broadcast(), "A could not broadcast");
					helper.assertTrue(radio(helper, B).search(), "B could not search");
				})
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, B).getPhase() == Phase.FOUND, "B has not found A: " + radio(helper, B).getPhase()))
				.thenExecute(() -> {
					helper.assertTrue(radio(helper, B).getCandidateName().equals("MWA" + RUN + "-1A2F"), "Found " + radio(helper, B).getCandidateName());
					radio(helper, B).confirm();
					helper.assertTrue(radio(helper, B).getPhase() == Phase.LINKED, "Not linked after confirming");
				})
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, A).isLinked() && radio(helper, A).getSubscribers().contains("MWB" + RUN + "-00F1"),
						"A does not list B as a subscriber"))
				.thenExecute(() -> dish(helper, A.east(2)).setAzimuth(270.0D)) // Turned right round: B is behind it. (Close in, a dish merely turned 90 degrees still links.)
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, B).getPhase() == Phase.LOST || radio(helper, B).getPhase() == Phase.SEARCHING,
						"B kept its link with A's dish turned away: " + radio(helper, B).getPhase()))
				.thenSucceed();
	}

	/**
	 * Over a link: an APRS packet heard at one H19 shows up at the other, and a phone call rings through and is answered.
	 * A batch of its own: run beside {@link #linkFoundConfirmedAndLost}, its B could find that test's broadcasting A.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH + "_relay")
	public static void aprsAndCallsCrossTheLink(GameTestHelper helper) {
		crossTheLink(helper, 6875, true);
	}

	/** A fax next to each radio: a map crosses the link in a couple of seconds, with no radio tones at all. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400, batch = BATCH + "_fax")
	public static void faxCrossesTheLink(GameTestHelper helper) {
		crossTheLink(helper, 6885, false);
	}

	private static void crossTheLink(GameTestHelper helper, int mhz, boolean aprs) {
		if(!aprs) {
			station(helper, A, 1);
			station(helper, B, -1);
			helper.setBlock(A.north(), RadiocraftBlocks.RADIO_FAXES.get("oak").get());
			helper.setBlock(B.north(), RadiocraftBlocks.RADIO_FAXES.get("birch").get());
			net.minecraft.world.item.ItemStack map = net.minecraft.world.item.MapItem.create(helper.getLevel(), 0, 0, (byte)0, false, false);
			helper.startSequence()
					.thenIdle(3)
					.thenExecute(() -> {
						link(helper, "MWE" + RUN, "MWF" + RUN);
						radio(helper, A).setFrequency(mhz);
						radio(helper, B).setFrequency(mhz);
					})
					.thenWaitUntil(() -> helper.assertTrue(radio(helper, A).isPowered() && radio(helper, B).isPowered() && radio(helper, A).getDishInfo().size() == 1,
							"Radios not up yet"))
					.thenExecute(() -> {
						radio(helper, A).broadcast();
						radio(helper, B).search();
					})
					.thenWaitUntil(() -> helper.assertTrue(radio(helper, B).getPhase() == Phase.FOUND, "B has not found A: " + radio(helper, B).getPhase()))
					.thenExecute(() -> {
						helper.assertTrue(radio(helper, B).getCandidateName().equals("MWE" + RUN + "-1A2F"), "B found " + radio(helper, B).getCandidateName());
						radio(helper, B).confirm();
					})
					.thenWaitUntil(() -> helper.assertTrue(radio(helper, A).isLinked(), "A is not linked; B is " + radio(helper, B).getPhase()))
					.thenExecute(() -> {
						RadioFaxBlockEntity here = GameTestUtil.blockEntity(helper, A.north(), RadioFaxBlockEntity.class);
						RadioFaxBlockEntity there = GameTestUtil.blockEntity(helper, B.north(), RadioFaxBlockEntity.class);
						here.setCallsign("W1LNK");
						there.setCallsign("W2LNK");
						here.getOutbox().setStackInSlot(0, map.copy());
						there.insertSupply(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MAP));
						helper.assertTrue(here.send("W2LNK") == null, "The fax could not call over the link");
						helper.assertTrue(here.getTransport() == RadioFaxBlockEntity.Transport.LINK, "The fax went by radio: " + here.getStatus());
					})
					.thenIdle(80) // Ten times VHF's twenty seconds, plus a little.
					.thenExecute(() -> {
						RadioFaxBlockEntity there = GameTestUtil.blockEntity(helper, B.north(), RadioFaxBlockEntity.class);
						helper.assertTrue(!there.getReceived().getStackInSlot(0).isEmpty(), "The map did not cross the link: " + there.getStatus() + " " + there.getPacketsDone());
					})
					.thenSucceed();
			return;
		}
		station(helper, A, 1);
		station(helper, B, -1);
		helper.setBlock(A.north(), RadiocraftBlocks.APRS_TERMINAL.get());
		helper.setBlock(B.north(), RadiocraftBlocks.APRS_TERMINAL.get());
		helper.setBlock(A.south(), RadiocraftBlocks.TELEPHONE.get());
		helper.setBlock(B.south(), RadiocraftBlocks.TELEPHONE.get());
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> {
					link(helper, "MWC" + RUN, "MWD" + RUN);
					radio(helper, A).setFrequency(mhz); // A channel of its own: dishes hear widely now, so no other test's radio answers.
					radio(helper, B).setFrequency(mhz);
				})
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, A).isPowered() && radio(helper, B).isPowered() && radio(helper, A).getDishInfo().size() == 1,
						"Radios not up yet"))
				.thenExecute(() -> {
					radio(helper, A).broadcast();
					radio(helper, B).search();
				})
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, B).getPhase() == Phase.FOUND, "B has not found A: " + radio(helper, B).getPhase()))
				.thenExecute(() -> {
					helper.assertTrue(radio(helper, B).getCandidateName().equals("MWC" + RUN + "-1A2F"), "B found " + radio(helper, B).getCandidateName());
					radio(helper, B).confirm();
				})
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, A).isLinked(), "A is not linked; B is " + radio(helper, B).getPhase()))
				.thenExecute(() -> {
					AprsTerminalBlockEntity here = GameTestUtil.blockEntity(helper, A.north(), AprsTerminalBlockEntity.class);
					here.receiveDirect(TncPacket.beacon(4242L, "W1TST", helper.absolutePos(A), "over the link", "HOUSE"));
					AprsTerminalBlockEntity there = GameTestUtil.blockEntity(helper, B.north(), AprsTerminalBlockEntity.class);
					helper.assertTrue(there.getBeacons().stream().anyMatch(heard -> heard.packet().from().equals("W1TST")), "The beacon did not cross the link");

					TelephoneBlockEntity caller = GameTestUtil.blockEntity(helper, A.south(), TelephoneBlockEntity.class);
					TelephoneBlockEntity callee = GameTestUtil.blockEntity(helper, B.south(), TelephoneBlockEntity.class);
					caller.dial(callee.getExtension());
					helper.assertTrue(callee.getState() == TelephoneBlockEntity.State.RINGING, "The far phone is not ringing: " + caller.getStatus());
					callee.answer();
					helper.assertTrue(caller.getState() == TelephoneBlockEntity.State.IN_CALL && callee.getState() == TelephoneBlockEntity.State.IN_CALL, "The call is not up");
					callee.hangUp();
					helper.assertTrue(caller.getState() == TelephoneBlockEntity.State.IDLE, "Hanging up did not end the call at the other end");
				})
				.thenSucceed();
	}

	/** Places the dish {@code step} along x past the coax as a player does: facing {@code yaw} toward the block, looking down at it. */
	private static void placeDish(GameTestHelper helper, BlockPos radio, int step, float yaw) {
		net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		player.setYRot(yaw);
		player.setXRot(40.0F); // Looking down at where the block goes.
		BlockPos at = helper.absolutePos(radio.offset(2 * step, 0, 0));
		net.minecraft.world.level.block.state.BlockState state = helper.getLevel().getBlockState(at);
		state.getBlock().setPlacedBy(helper.getLevel(), at, state, player, new net.minecraft.world.item.ItemStack(RadiocraftBlocks.SATELLITE_DISH.get()));
	}

	/**
	 * Dishes placed by a player standing between them link without touching them: a placed dish faces back at the player
	 * and stays level, rather than tilting down where they looked. A batch of its own, as the relay test.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH + "_placed")
	public static void placedDishesLinkAsTheyStand(GameTestHelper helper) {
		station(helper, A, 1);
		station(helper, B, -1);
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> {
					placeDish(helper, A, 1, 90.0F); // The player between them looks west at A's dish, which faces back east, toward B.
					placeDish(helper, B, -1, -90.0F); // Then east at B's, which faces west, toward A.
					helper.assertTrue(dish(helper, A.east(2)).getElevation() == 0.0F, "A placed dish is tilted " + dish(helper, A.east(2)).getElevation());
					helper.assertTrue(Math.abs(dish(helper, A.east(2)).getAzimuth() - 90.0F) < 0.01F, "A's dish faces " + dish(helper, A.east(2)).getAzimuth());
					RadiocraftServerConfig.MICROWAVE_SEARCH_SECONDS.set(1);
					MicrowaveRadioBlockEntity a = radio(helper, A), b = radio(helper, B);
					a.togglePower();
					b.togglePower();
					helper.assertTrue(a.setIdentity("MWE" + RUN, 0x2B3C), "A could not take its callsign");
					helper.assertTrue(b.setIdentity("MWF" + RUN, 0x3C4D), "B could not take its callsign");
				})
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, A).isPowered() && radio(helper, B).isPowered() && radio(helper, A).getDishInfo().size() == 1,
						"Radios not up yet"))
				.thenExecute(() -> {
					radio(helper, A).broadcast();
					radio(helper, B).search();
				})
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, B).getPhase() == Phase.FOUND, "B has not found A: " + radio(helper, B).getPhase()
						+ " " + radio(helper, B).getProblem()))
				.thenExecute(() -> radio(helper, B).confirm())
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, A).isLinked(), "A is not linked"))
				.thenSucceed();
	}

	/** The power switch survives a reload: networks live in memory only, so the block entity's save puts it back. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40, batch = BATCH)
	public static void switchSurvivesAReload(GameTestHelper helper) {
		helper.setBlock(A, RadiocraftBlocks.MICROWAVE_RADIO.get());
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					MicrowaveRadioBlockEntity radio = radio(helper, A);
					radio.togglePower();
					net.minecraft.nbt.CompoundTag saved = radio.saveWithoutMetadata(helper.getLevel().registryAccess());
					BlockPos at = helper.absolutePos(A);
					com.arrl.radiocraft.api.capabilities.IBENetworks.removeObject(helper.getLevel(), at); // As after a restart.
					radio.loadWithComponents(saved, helper.getLevel().registryAccess());
					helper.assertTrue(radio.getNetworkObject(helper.getLevel(), at) instanceof com.arrl.radiocraft.common.be_networks.network_objects.MicrowaveNetworkObjects.Radio object
							&& object.wantsOn, "The switch came back off");
				})
				.thenSucceed();
	}

	/** Two radios cannot share a callsign and ID; a different ID is fine. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40, batch = BATCH)
	public static void identitiesAreUnique(GameTestHelper helper) {
		helper.setBlock(A, RadiocraftBlocks.MICROWAVE_RADIO.get());
		helper.setBlock(B, RadiocraftBlocks.MICROWAVE_RADIO.get());
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					helper.assertTrue(radio(helper, A).setIdentity("MWU" + RUN, 0x0001), "The first radio could not take the pair");
					helper.assertFalse(radio(helper, B).setIdentity("MWU" + RUN, 0x0001), "The second radio took the same pair");
					helper.assertTrue(radio(helper, B).setIdentity("MWU" + RUN, 0x0002), "A different ID was refused");
				})
				.thenSucceed();
	}

	/** A station on the air is in the transmitter log, where the handheld direction finder looks. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 60, batch = BATCH)
	public static void transmissionsAreLogged(GameTestHelper helper) {
		BlockPos station = new BlockPos(4, 1, 4);
		GameTestUtil.radioWithJPole(helper, station);
		float hz = 147_340_000.0F;
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> {
					RadioBlockEntity radio = GameTestUtil.blockEntity(helper, station, RadioBlockEntity.class);
					radio.setFrequency(hz);
					radio.toggle();
				})
				.thenWaitUntil(() -> {
					RadioBlockEntity radio = GameTestUtil.blockEntity(helper, station, RadioBlockEntity.class);
					radio.setPTTDown(true);
					radio.acceptVoicePacket(() -> helper.getLevel(), new short[960], UUID.nameUUIDFromBytes("gametest-txlog".getBytes()));
					radio.setPTTDown(false);
					TransmitterLog.Entry heard = TransmitterLog.nearest(helper.getLevel(), helper.absolutePos(station), hz, 10_000.0D, 64.0D, 5L);
					helper.assertTrue(heard != null, "The transmission is not in the log");
				})
				.thenSucceed();
	}
}
