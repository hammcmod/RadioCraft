package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blockentities.AprsTerminalBlockEntity;
import com.arrl.radiocraft.common.blockentities.MicrowaveDishBlockEntity;
import com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity;
import com.arrl.radiocraft.common.radio.microwave.MicrowaveMath;
import com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity.Phase;
import com.arrl.radiocraft.common.blockentities.RadioFaxBlockEntity;
import com.arrl.radiocraft.common.blockentities.TelephoneBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.TransmitterLog;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.level.block.Blocks;
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

	/**
	 * Coax touching any of the horn's four base blocks reaches its feed (the block entity in the placed corner), drawn
	 * across the base to where the four meet; coax beside the horn higher up does not connect at all.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 60, batch = BATCH + "_hornfeed")
	public static void coaxAtAnyBaseBlockFeedsTheHorn(GameTestHelper helper) {
		// World coordinates throughout: a test plot may be rotated, and the horn's parts are laid out in the world.
		net.minecraft.server.level.ServerLevel level = helper.getLevel();
		com.arrl.radiocraft.common.blocks.MicrowaveHornBlock horn = RadiocraftBlocks.MICROWAVE_HORN.get();
		net.minecraft.core.Direction facing = net.minecraft.core.Direction.EAST;
		BlockPos origin = helper.absolutePos(new BlockPos(7, 1, 7));
		net.minecraft.world.level.block.state.BlockState state = horn.defaultBlockState().setValue(com.arrl.radiocraft.common.blocks.MicrowaveHornBlock.FACING, facing);
		level.setBlockAndUpdate(origin, state);
		horn.setPlacedBy(level, origin, state, null, new net.minecraft.world.item.ItemStack(com.arrl.radiocraft.common.init.RadiocraftItems.MICROWAVE_HORN.get()));
		java.util.Set<BlockPos> hornBlocks = new java.util.HashSet<>();
		BlockPos found = null;
		for(var part : horn.parts(origin, facing)) {
			hornBlocks.add(part.getKey());
			if(part.getValue().getValue(com.arrl.radiocraft.common.blocks.MicrowaveHornBlock.PART_Y) == 0 && !part.getKey().equals(origin))
				found = part.getKey(); // A base block that is not the one with the block entity.
		}
		helper.assertTrue(found != null, "No other base block");
		BlockPos base = found;
		net.minecraft.core.Direction outward = null; // The side of that base block facing out of the horn.
		for(net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL)
			if(!hornBlocks.contains(base.relative(dir)))
				outward = dir;
		helper.assertTrue(outward != null, "The base block has no outside");
		net.minecraft.core.Direction out = outward, in = outward.getOpposite();
		BlockPos coax = base.relative(out), radioPos = base.relative(out, 2);
		level.setBlockAndUpdate(radioPos, RadiocraftBlocks.MICROWAVE_RADIO.get().defaultBlockState());
		com.arrl.radiocraft.common.blocks.WireBlock wire = RadiocraftBlocks.COAX_WIRE.get();
		level.setBlockAndUpdate(coax, wire.getBlockState(level, coax));
		helper.succeedWhen(() -> {
			MicrowaveRadioBlockEntity radio = (MicrowaveRadioBlockEntity)level.getBlockEntity(radioPos);
			helper.assertTrue(radio != null && radio.dishes(level).size() == 1, "The radio does not reach the horn through a base block");
			java.util.Set<BlockPos> onTheCoax = new java.util.HashSet<>(); // What the coax's Jade tooltip lists.
			com.arrl.radiocraft.compat.CoaxWireProvider.collectConnections(level, coax, onTheCoax);
			helper.assertTrue(onTheCoax.contains(origin), "The coax tooltip does not see the horn through a base block: " + onTheCoax);
			helper.assertFalse(wire.canConnectTo(level, base.above()), "Coax would connect to the horn's second layer");
			MicrowaveDishBlockEntity feedEnd = (MicrowaveDishBlockEntity)level.getBlockEntity(origin);
			feedEnd.setElevation(80.0D);
			helper.assertTrue(feedEnd.getElevation() == 45.0F, "The horn tilts past 45 degrees: " + feedEnd.getElevation());
			feedEnd.setElevation(-80.0D);
			helper.assertTrue(feedEnd.getElevation() == -45.0F, "The horn tilts past -45 degrees: " + feedEnd.getElevation());
			feedEnd.setElevation(0.0D);
			var look = wire.look(level, coax, level.getBlockState(coax));
			helper.assertTrue(look.feed() == in && Math.abs(look.feedSide()) == 8, "The coax does not run to the feed: " + look.feed() + " " + look.feedSide());
		});
	}

	/**
	 * A radio with coax up a two-block pole to a dish on top, at {@code radio}, the pole {@code step} blocks along x and
	 * the dish pointing {@code azimuth}: the maintainer's yard layout of 2026-10-27.
	 */
	private static BlockPos poleStation(GameTestHelper helper, BlockPos radio, int step, double azimuth) {
		helper.setBlock(radio, RadiocraftBlocks.MICROWAVE_RADIO.get());
		BlockPos foot = radio.offset(step, 0, 0), pole = radio.offset(2 * step, 0, 0);
		helper.setBlock(pole, Blocks.STONE);
		helper.setBlock(pole.above(), Blocks.STONE);
		GameTestUtil.placeWire(helper, foot, RadiocraftBlocks.COAX_WIRE.get());
		GameTestUtil.placeWire(helper, foot.above(), RadiocraftBlocks.COAX_WIRE.get()); // Climbs onto the pole's top.
		BlockPos top = pole.above(2);
		helper.setBlock(top, RadiocraftBlocks.SATELLITE_DISH.get());
		helper.runAfterDelay(1, () -> dish(helper, top).setAzimuth(azimuth));
		return top;
	}

	/**
	 * Two dishes on short poles nine blocks apart see each other over a one-block step in the ground between them (the
	 * terrain atlas used to call that blocked), and not through a spruce tree in the beam.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = BATCH + "_path")
	public static void beamClearsAStepButNotATree(GameTestHelper helper) {
		BlockPos a = new BlockPos(1, 1, 4), b = new BlockPos(14, 1, 4);
		poleStation(helper, a, 1, 90.0D);
		poleStation(helper, b, -1, 270.0D);
		helper.setBlock(new BlockPos(7, 1, 4), Blocks.STONE); // The step, under the beam.
		helper.setBlock(new BlockPos(8, 1, 4), Blocks.STONE);
		helper.startSequence()
				.thenIdle(4)
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, a).getDishInfo().size() == 1 && radio(helper, b).getDishInfo().size() == 1, "A dish is missing from its coax"))
				.thenExecute(() -> {
					MicrowaveMath.Signal signal = MicrowaveRadioBlockEntity.bestSignal(helper.getLevel(), radio(helper, a), radio(helper, b));
					helper.assertTrue(signal.result() == MicrowaveMath.Result.OK, "Over a one-block step the beam reads " + signal.result());
					helper.setBlock(new BlockPos(8, 2, 4), Blocks.SPRUCE_LOG);
					helper.setBlock(new BlockPos(8, 3, 4), Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
				})
				.thenExecute(() -> {
					MicrowaveMath.Signal signal = MicrowaveRadioBlockEntity.bestSignal(helper.getLevel(), radio(helper, a), radio(helper, b));
					helper.assertTrue(signal.result() == MicrowaveMath.Result.BLOCKED, "Through a tree the beam reads " + signal.result());
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
	/**
	 * Three sites in a chain: A links to B1, B2 sits on top of B1 (one site: one data network) and links on to C. A phone
	 * at A rings a phone at C, two links away; with {@code microwave_max_hops} at 1 it cannot.
	 *
	 * <pre>
	 *   A (2,1,3) coax, dish east  ->  dish west, coax, B1 (13,1,3)        link one, 6905 MHz
	 *                                   B2 (13,2,3), coax up, dish south  ->  dish north, coax, C (13,4,15)   link two, 6995 MHz
	 * </pre>
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400, batch = BATCH + "_hops")
	public static void sitesRelayAcrossTwoLinks(GameTestHelper helper) {
		BlockPos a = new BlockPos(2, 1, 3), b1 = new BlockPos(13, 1, 3), b2 = b1.above(), c = new BlockPos(13, 4, 15);
		station(helper, a, 1);
		station(helper, b1, -1);
		helper.setBlock(b2, RadiocraftBlocks.MICROWAVE_RADIO.get());
		GameTestUtil.placeWire(helper, b2.above(), RadiocraftBlocks.COAX_WIRE.get());
		helper.setBlock(b2.above(2), RadiocraftBlocks.SATELLITE_DISH.get());
		helper.setBlock(c, RadiocraftBlocks.MICROWAVE_RADIO.get());
		GameTestUtil.placeWire(helper, c.north(), RadiocraftBlocks.COAX_WIRE.get());
		helper.setBlock(c.north(2), RadiocraftBlocks.SATELLITE_DISH.get());
		helper.setBlock(a.north(), RadiocraftBlocks.TELEPHONE.get());
		helper.setBlock(c.west(), RadiocraftBlocks.TELEPHONE.get());
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> {
					RadiocraftServerConfig.MICROWAVE_SEARCH_SECONDS.set(1);
					dish(helper, a.east(2)).setAzimuth(90.0D);
					dish(helper, b1.west(2)).setAzimuth(270.0D);
					dish(helper, b2.above(2)).setAzimuth(180.0D);
					dish(helper, c.north(2)).setAzimuth(0.0D);
					String[] calls = { "MWG", "MWH", "MWI", "MWJ" };
					BlockPos[] at = { a, b1, b2, c };
					int[] mhz = { 6905, 6905, 6995, 6995 };
					for(int i = 0; i < 4; i++) {
						MicrowaveRadioBlockEntity radio = radio(helper, at[i]);
						radio.togglePower();
						helper.assertTrue(radio.setIdentity(calls[i] + RUN, 0x0A10 + i), calls[i] + " could not take its callsign");
						radio.setFrequency(mhz[i]);
					}
				})
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, a).isPowered() && radio(helper, c).isPowered()
						&& radio(helper, b2).getDishInfo().size() == 1 && radio(helper, b1).getDishInfo().size() == 1, "Radios not up yet"))
				.thenExecute(() -> {
					radio(helper, a).broadcast();
					radio(helper, b2).broadcast();
					radio(helper, b1).search();
					radio(helper, c).search();
				})
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, b1).getPhase() == Phase.FOUND && radio(helper, c).getPhase() == Phase.FOUND,
						"Not found yet: B1 " + radio(helper, b1).getPhase() + ", C " + radio(helper, c).getPhase()))
				.thenExecute(() -> {
					radio(helper, b1).confirm();
					radio(helper, c).confirm();
				})
				.thenWaitUntil(() -> helper.assertTrue(radio(helper, a).isLinked() && radio(helper, b2).isLinked(), "Links not up yet"))
				.thenExecute(() -> {
					TelephoneBlockEntity caller = GameTestUtil.blockEntity(helper, a.north(), TelephoneBlockEntity.class);
					TelephoneBlockEntity callee = GameTestUtil.blockEntity(helper, c.west(), TelephoneBlockEntity.class);
					// Hop limit 1: C is two links away. Put the limit back before any assert can leave it set.
					RadiocraftServerConfig.MICROWAVE_MAX_HOPS.set(1);
					boolean reachedAtOne = caller.reachable().contains(callee);
					RadiocraftServerConfig.MICROWAVE_MAX_HOPS.set(8);
					helper.assertFalse(reachedAtOne, "A phone two links away was reachable with a hop limit of one");
					caller.dial(callee.getExtension());
					helper.assertTrue(callee.getState() == TelephoneBlockEntity.State.RINGING, "The phone two links away is not ringing: " + caller.getStatus());
					callee.answer();
					helper.assertTrue(caller.getState() == TelephoneBlockEntity.State.IN_CALL, "The relayed call is not up");
					callee.hangUp();
				})
				.thenSucceed();
	}

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
