package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.DirectionFinderBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.blocks.DirectionFinderModuleBlock;
import com.arrl.radiocraft.common.blocks.MicrowaveHornBlock;
import com.arrl.radiocraft.common.blocks.TowerBeamBlock;
import com.arrl.radiocraft.common.blocks.antennas.DoubleVHFAntennaBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.radio.BEVoiceReceiver;
import com.arrl.radiocraft.common.radio.DirectionFinding;
import com.arrl.radiocraft.common.radio.voice.SpeakerKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The blocks registered 2026-10-26: tower beams join every beam they touch, the microwave horn goes up and comes
 * down whole, speakers change what a radio plays through, and a direction finder locks on and points at a
 * transmitter.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class NewBlocksGameTests {

	private static final String DF_BATCH = "direction_finder";
	/** 2m frequency only these tests use. */
	private static final float DF_HZ = 147_580_000.0F;
	private static final UUID OPERATOR = UUID.nameUUIDFromBytes("gametest-direction-finder".getBytes());

	private static int bit(String name) {
		for(int i = 0; i < TowerBeamBlock.OFFSETS.size(); i++)
			if(TowerBeamBlock.OFFSETS.get(i).name().equals(name))
				return 1 << i;
		throw new IllegalArgumentException(name);
	}

	/** A beam sees beams above it and on a diagonal corner, and nothing where there is none. */
	@GameTest(template = GameTestUtil.EMPTY)
	public static void towerBeamsJoinTouchingBeams(GameTestHelper helper) {
		BlockPos beam = new BlockPos(4, 1, 4);
		helper.setBlock(beam, RadiocraftBlocks.TOWER_BEAM.get());
		helper.setBlock(beam.above(), RadiocraftBlocks.TOWER_BEAM_THIN.get());
		helper.setBlock(beam.above().south().east(), RadiocraftBlocks.TOWER_BEAM_THICK.get());
		int mask = TowerBeamBlock.connections(helper.getLevel(), helper.absolutePos(beam));
		helper.assertTrue(mask == (bit("up") | bit("up_south_east")), "Expected up and up_south_east, got mask " + Integer.toBinaryString(mask));
		helper.assertTrue(TowerBeamBlock.OFFSETS.size() == 26, "26 neighbours");
		helper.succeed();
	}

	/** Placed, all sixteen parts go up; one part broken, the whole horn comes down and drops one horn. */
	@GameTest(template = GameTestUtil.EMPTY)
	public static void microwaveHornGoesUpAndComesDownWhole(GameTestHelper helper) {
		BlockPos origin = new BlockPos(6, 1, 6);
		MicrowaveHornBlock horn = RadiocraftBlocks.MICROWAVE_HORN.get();
		BlockState state = horn.defaultBlockState().setValue(MicrowaveHornBlock.FACING, Direction.EAST);
		helper.setBlock(origin, state);
		BlockPos absOrigin = helper.absolutePos(origin);
		horn.setPlacedBy(helper.getLevel(), absOrigin, state, null, new ItemStack(RadiocraftItems.MICROWAVE_HORN.get()));
		List<Map.Entry<BlockPos, BlockState>> parts = horn.parts(absOrigin, Direction.EAST);
		for(Map.Entry<BlockPos, BlockState> part : parts)
			helper.assertTrue(helper.getLevel().getBlockState(part.getKey()).equals(part.getValue()), "Part missing at " + part.getKey());
		BlockPos top = parts.stream().filter(part -> part.getValue().getValue(MicrowaveHornBlock.PART_Y) == 3).findFirst().orElseThrow().getKey();
		helper.getLevel().destroyBlock(top, true);
		for(Map.Entry<BlockPos, BlockState> part : parts)
			helper.assertTrue(helper.getLevel().getBlockState(part.getKey()).isAir(), "Part left standing at " + part.getKey());
		helper.assertItemEntityCountIs(RadiocraftItems.MICROWAVE_HORN.get(), origin, 6.0D, 1);
		helper.succeed();
	}

	/** A radio plays through its own speaker, a standard one touching it, or better, a premium one. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 120)
	public static void speakerTouchingTheRadioIsUsed(GameTestHelper helper) {
		BlockPos radio = new BlockPos(4, 1, 4);
		helper.setBlock(radio, RadiocraftBlocks.HF_RADIO_20M.get());
		helper.startSequence()
				.thenIdle(25)
				.thenExecute(() -> helper.assertTrue(receiver(helper, radio).getSpeaker() == SpeakerKind.BUILT_IN, "Expected the built-in speaker"))
				.thenExecute(() -> helper.setBlock(radio.east(), RadiocraftBlocks.DESK_SPEAKERS.get("oak").get()))
				.thenWaitUntil(() -> helper.assertTrue(receiver(helper, radio).getSpeaker() == SpeakerKind.STANDARD, "The desk speaker is not used"))
				.thenExecute(() -> helper.setBlock(radio.above(), RadiocraftBlocks.PREMIUM_SPEAKER_GOLD.get()))
				.thenWaitUntil(() -> helper.assertTrue(receiver(helper, radio).getSpeaker() == SpeakerKind.PREMIUM, "The premium speaker does not win"))
				.thenSucceed();
	}

	/** Wall speakers and loudspeakers on the radio's wire run each play the radio. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 120)
	public static void wiredSpeakersOnTheRadiosWire(GameTestHelper helper) {
		BlockPos radio = new BlockPos(3, 1, 4);
		helper.setBlock(radio, RadiocraftBlocks.HF_RADIO_20M.get());
		helper.setBlock(radio.east(3), RadiocraftBlocks.WALL_SPEAKER.get());
		helper.setBlock(radio.east(2).south(), RadiocraftBlocks.LOUDSPEAKER.get());
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					GameTestUtil.placeWire(helper, radio.east(), RadiocraftBlocks.WIRE.get());
					GameTestUtil.placeWire(helper, radio.east(2), RadiocraftBlocks.WIRE.get());
				})
				.thenWaitUntil(() -> {
					List<BEVoiceReceiver.WiredSpeaker> wired = receiver(helper, radio).getWiredSpeakers();
					helper.assertTrue(wired.size() == 2, "Expected two wired speakers, found " + wired);
					helper.assertTrue(wired.stream().anyMatch(s -> s.kind() == SpeakerKind.WALL) && wired.stream().anyMatch(s -> s.kind() == SpeakerKind.LOUDSPEAKER),
							"Expected a wall speaker and a loudspeaker, found " + wired);
				})
				.thenSucceed();
	}

	private static BEVoiceReceiver receiver(GameTestHelper helper, BlockPos radio) {
		return GameTestUtil.blockEntity(helper, radio, RadioBlockEntity.class).getVoiceReceiver();
	}

	/** A module with coax running out of it {@code outward} to a J-pole. */
	private static void module(GameTestHelper helper, BlockPos at, Direction outward, boolean antenna) {
		helper.setBlock(at, RadiocraftBlocks.DIRECTION_FINDER_MODULE.get());
		GameTestUtil.placeWire(helper, at.relative(outward), RadiocraftBlocks.COAX_WIRE.get());
		if(!antenna)
			return;
		BlockPos lower = at.relative(outward, 2);
		helper.setBlock(lower, RadiocraftBlocks.J_POLE_ANTENNA.get().defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.LOWER));
		helper.setBlock(lower.above(), RadiocraftBlocks.J_POLE_ANTENNA.get().defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.UPPER));
	}

	private static short[] tone() {
		short[] audio = new short[960];
		for(int i = 0; i < audio.length; i++)
			audio[i] = (short)(Math.sin(2 * Math.PI * 440 * i / 48000.0D) * 8000);
		return audio;
	}

	/**
	 * Four modules round a direction finder, each with a J-pole; a VHF station talks on the frequency. The modules lock
	 * on one at a time, and the bearing it gives is within four of its own error of the true bearing.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400, batch = DF_BATCH)
	public static void directionFinderLocksOnAndPoints(GameTestHelper helper) {
		BlockPos finder = new BlockPos(5, 1, 5);
		helper.setBlock(finder, RadiocraftBlocks.DIRECTION_FINDER.get());
		module(helper, finder.west(), Direction.WEST, true);
		module(helper, finder.east(), Direction.EAST, true);
		module(helper, finder.north(), Direction.NORTH, true);
		module(helper, finder.south(), Direction.SOUTH, true);
		BlockPos station = new BlockPos(13, 1, 12);
		GameTestUtil.radioWithJPole(helper, station); // J-pole at (13, 1, 14).
		BlockPos jPole = helper.absolutePos(station.south(2));
		BlockPos absFinder = helper.absolutePos(finder);
		double truth = DirectionFinding.bearing(absFinder.getX() + 0.5D, absFinder.getZ() + 0.5D, jPole.getX() + 0.5D, jPole.getZ() + 0.5D);
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> {
					DirectionFinderBlockEntity df = GameTestUtil.blockEntity(helper, finder, DirectionFinderBlockEntity.class);
					df.setFrequency(DF_HZ);
					df.togglePower();
					RadioBlockEntity radio = GameTestUtil.blockEntity(helper, station, RadioBlockEntity.class);
					radio.setFrequency(DF_HZ);
					if(!GameTestUtil.networkObject(helper, station, RadioNetworkObject.class).isPowered)
						radio.toggle();
				})
				.thenWaitUntil(() -> {
					DirectionFinderBlockEntity df = GameTestUtil.blockEntity(helper, finder, DirectionFinderBlockEntity.class);
					helper.assertTrue(df.usableModules() == 4, "Expected 4 usable modules, have " + df.usableModules() + " " + df.getStatuses());
				})
				.thenWaitUntil(() -> {
					RadioBlockEntity radio = GameTestUtil.blockEntity(helper, station, RadioBlockEntity.class);
					radio.setPTTDown(true);
					radio.acceptVoicePacket(() -> helper.getLevel(), tone(), OPERATOR);
					DirectionFinderBlockEntity df = GameTestUtil.blockEntity(helper, finder, DirectionFinderBlockEntity.class);
					helper.assertTrue(df.getLocked() == 4, "Locked " + df.getLocked() + " of 4, hearing " + df.isHearing());
				})
				.thenExecute(() -> {
					GameTestUtil.blockEntity(helper, station, RadioBlockEntity.class).setPTTDown(false);
					DirectionFinderBlockEntity df = GameTestUtil.blockEntity(helper, finder, DirectionFinderBlockEntity.class);
					helper.assertTrue(df.hasBearing(), "No bearing with four modules locked");
					helper.assertTrue(df.getSigma() == DirectionFinding.sigma(4, false, false), "Expected the four-module accuracy, got " + df.getSigma());
					double off = DirectionFinding.difference(df.getBearing(), truth);
					helper.assertTrue(off <= 4.0D * df.getSigma(), "Bearing " + df.getBearing() + " is " + off + " degrees off the true " + truth);
					helper.assertTrue(helper.getBlockState(finder.west()).getValue(DirectionFinderModuleBlock.LAMP) == DirectionFinderModuleBlock.Lamp.GREEN,
							"A locked module's lamp is not green");
				})
				.thenSucceed();
	}

	/** Two modules with antennas and one without: not enough to lock, and the bare one shows red. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = DF_BATCH)
	public static void directionFinderNeedsThreeMatchingModules(GameTestHelper helper) {
		BlockPos finder = new BlockPos(5, 1, 5);
		helper.setBlock(finder, RadiocraftBlocks.DIRECTION_FINDER.get());
		module(helper, finder.west(), Direction.WEST, true);
		module(helper, finder.east(), Direction.EAST, true);
		module(helper, finder.north(), Direction.NORTH, false);
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> GameTestUtil.blockEntity(helper, finder, DirectionFinderBlockEntity.class).togglePower())
				.thenWaitUntil(() -> {
					DirectionFinderBlockEntity df = GameTestUtil.blockEntity(helper, finder, DirectionFinderBlockEntity.class);
					helper.assertTrue(df.getModules().size() == 3, "Expected three modules attached, have " + df.getModules().size());
					helper.assertTrue(df.getStatuses().contains(DirectionFinderBlockEntity.ModuleStatus.NO_ANTENNA), "The bare module is not flagged: " + df.getStatuses());
					helper.assertTrue(df.usableModules() == 2, "Expected 2 usable modules, have " + df.usableModules());
					helper.assertTrue(helper.getBlockState(finder.north()).getValue(DirectionFinderModuleBlock.LAMP) == DirectionFinderModuleBlock.Lamp.RED,
							"The bare module's lamp is not red");
				})
				.thenSucceed();
	}
}
