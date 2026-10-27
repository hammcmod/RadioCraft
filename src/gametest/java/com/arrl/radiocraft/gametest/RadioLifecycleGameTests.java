package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.AntennaBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.api.antenna.AntennaTypes;
import com.arrl.radiocraft.common.blockentities.CommercialRadioModuleBlockEntity;
import com.arrl.radiocraft.common.blocks.StackedBlocks;
import com.arrl.radiocraft.common.blocks.antennas.DoubleVHFAntennaBlock;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.voice.StationProgram;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * Radios keep hearing after what happens to a station in play: its coax broken and laid again (or dyed in place), the
 * radio broken and put back, the antenna reloaded as after a chunk reload, and a stale antenna left on the network from
 * another level (a world left and rejoined in the same game).
 * <pre>
 *   z=2 radio A   radio B (x+6)
 *   z=3 coax      coax
 *   z=4 J-pole    J-pole
 * </pre>
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class RadioLifecycleGameTests {

	private static final String BATCH = "radio_lifecycle";
	private static final BlockPos A = new BlockPos(3, 2, 2), B = new BlockPos(9, 2, 2);
	private static final UUID OPERATOR = UUID.nameUUIDFromBytes("gametest-lifecycle".getBytes());

	private static void stations(GameTestHelper helper) {
		GameTestUtil.radioWithJPole(helper, A);
		GameTestUtil.radioWithJPole(helper, B);
	}

	private static RadioBlockEntity radio(GameTestHelper helper, BlockPos pos) {
		return GameTestUtil.blockEntity(helper, pos, RadioBlockEntity.class);
	}

	/** Both radios on {@code hz}, switched on. */
	private static void tune(GameTestHelper helper, float hz) {
		for(BlockPos pos : new BlockPos[] { A, B }) {
			RadioBlockEntity radio = radio(helper, pos);
			radio.setFrequency(hz);
			if(!GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class).isPowered)
				radio.toggle();
		}
	}

	private static short[] tone() {
		short[] audio = new short[960];
		for(int i = 0; i < audio.length; i++)
			audio[i] = (short)(Math.sin(2 * Math.PI * 440 * i / 48000.0D) * 8000);
		return audio;
	}

	/** B talks; A must record it (a fresh reception, after {@code since}). */
	private static void assertAHearsB(GameTestHelper helper, long since, String when) {
		RadioBlockEntity b = radio(helper, B);
		b.setPTTDown(true);
		b.acceptVoicePacket(() -> helper.getLevel(), tone(), OPERATOR);
		b.setPTTDown(false);
		RadioBlockEntity a = radio(helper, A);
		var antennas = GameTestUtil.networkObject(helper, A, RadioNetworkObject.class).getAntennas();
		helper.assertTrue(a.getLastSignalGameTime() > since, "Radio A does not hear B " + when + ": antennas on its coax " + antennas.size()
				+ (antennas.isEmpty() ? "" : ", radios on that antenna " + antennas.getFirst().getRadios().size()
				+ ", antenna recognised " + (antennas.getFirst().getAntenna() != null)));
	}

	private static long now(GameTestHelper helper) {
		return helper.getLevel().getGameTime();
	}

	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
	public static void hearsAfterTheCoaxIsLaidAgain(GameTestHelper helper) {
		stations(helper);
		long[] mark = new long[1];
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> tune(helper, 146_700_000.0F))
				.thenWaitUntil(() -> assertAHearsB(helper, -1L, "at first"))
				.thenExecute(() -> helper.setBlock(A.south(), Blocks.AIR))
				.thenIdle(2)
				.thenExecute(() -> GameTestUtil.placeWire(helper, A.south(), RadiocraftBlocks.COAX_WIRE.get()))
				.thenIdle(2)
				.thenExecute(() -> mark[0] = now(helper))
				.thenWaitUntil(() -> assertAHearsB(helper, mark[0], "after its coax was broken and laid again"))
				.thenSucceed();
	}

	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
	public static void hearsAfterTheCoaxIsDyedInPlace(GameTestHelper helper) {
		stations(helper);
		long[] mark = new long[1];
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> tune(helper, 146_720_000.0F))
				.thenWaitUntil(() -> assertAHearsB(helper, -1L, "at first"))
				.thenExecute(() -> GameTestUtil.placeWire(helper, A.south(), RadiocraftBlocks.COLOURED_COAX.get(DyeColor.RED).get()))
				.thenIdle(2)
				.thenExecute(() -> GameTestUtil.placeWire(helper, A.south(), RadiocraftBlocks.COLOURED_COAX.get(DyeColor.BLUE).get()))
				.thenIdle(2)
				.thenExecute(() -> mark[0] = now(helper))
				.thenWaitUntil(() -> assertAHearsB(helper, mark[0], "after its coax was dyed in place twice"))
				.thenSucceed();
	}

	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
	public static void hearsAfterTheRadioIsPutBack(GameTestHelper helper) {
		stations(helper);
		long[] mark = new long[1];
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> tune(helper, 146_740_000.0F))
				.thenWaitUntil(() -> assertAHearsB(helper, -1L, "at first"))
				.thenExecute(() -> helper.setBlock(A, Blocks.AIR))
				.thenIdle(2)
				.thenExecute(() -> helper.setBlock(A, RadiocraftBlocks.VHF_BASE_STATION.get()))
				.thenIdle(3)
				.thenExecute(() -> tune(helper, 146_740_000.0F))
				.thenExecute(() -> mark[0] = now(helper))
				.thenWaitUntil(() -> assertAHearsB(helper, mark[0], "after it was broken and put back"))
				.thenSucceed();
	}

	/** The J-pole's block entity loads again, as after its chunk unloads and reloads. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
	public static void hearsAfterTheAntennaReloads(GameTestHelper helper) {
		stations(helper);
		long[] mark = new long[1];
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> tune(helper, 146_760_000.0F))
				.thenWaitUntil(() -> assertAHearsB(helper, -1L, "at first"))
				.thenExecute(() -> {
					for(BlockPos pos : new BlockPos[] { A.south(2), B.south(2) }) {
						AntennaBlockEntity antenna = GameTestUtil.blockEntity(helper, pos, AntennaBlockEntity.class);
						antenna.onChunkUnloaded();
						antenna.onLoad();
					}
				})
				.thenIdle(2)
				.thenExecute(() -> mark[0] = now(helper))
				.thenWaitUntil(() -> assertAHearsB(helper, mark[0], "after the antennas reloaded"))
				.thenSucceed();
	}

	/**
	 * An antenna left on the network from another level, at the same coordinates as A's (a world left and joined again
	 * without restarting the game leaves the old world's antennas behind): B's signal still reaches A.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
	public static void staleAntennaFromAnotherLevelIsHarmless(GameTestHelper helper) {
		stations(helper);
		ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
		StaticAntenna<?> ghost = new StaticAntenna<>(AntennaTypes.getType(Radiocraft.id("j_pole")), helper.absolutePos(A.south(2)), nether);
		long[] mark = new long[1];
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> {
					AntennaNetworkManager.getNetwork(AntennaNetworkManager.VHF_ID).addAntenna(ghost);
					tune(helper, 146_780_000.0F);
					mark[0] = now(helper);
				})
				.thenWaitUntil(() -> assertAHearsB(helper, mark[0], "with a stale antenna from another level on the network"))
				.thenExecute(() -> AntennaNetworkManager.getNetwork(AntennaNetworkManager.VHF_ID).removeAntenna(ghost))
				.thenSucceed();
	}

	/** A's antenna drops off the antenna network (as a stale entry might): it puts itself back within a few seconds. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
	public static void antennaRejoinsItsNetwork(GameTestHelper helper) {
		stations(helper);
		long[] mark = new long[1];
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> tune(helper, 146_800_000.0F))
				.thenWaitUntil(() -> assertAHearsB(helper, -1L, "at first"))
				.thenExecute(() -> {
					AntennaNetworkObject antenna = GameTestUtil.networkObject(helper, A.south(2), AntennaNetworkObject.class);
					AntennaNetworkManager.getNetwork(AntennaNetworkManager.VHF_ID).removeAntenna(antenna.getAntenna());
					mark[0] = now(helper);
				})
				.thenWaitUntil(() -> assertAHearsB(helper, mark[0], "after its antenna fell off the network"))
				.thenSucceed();
	}

	/**
	 * The user's station: a J-pole put down by a player (the upper half placed by setPlacedBy), the coax laid after it,
	 * a solar panel over the mast, and a commercial module beeping on 144.200 MHz a dozen blocks off.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 160, batch = BATCH)
	public static void playerPlacedJPoleHearsTheModule(GameTestHelper helper) {
		BlockPos module = new BlockPos(2, 2, 12), radio = new BlockPos(12, 2, 2), jPole = radio.south(2);
		for(int i = 0; i < StackedBlocks.HEIGHT; i++)
			helper.setBlock(module.above(i), RadiocraftBlocks.COMMERCIAL_RADIO_MODULE.get().defaultBlockState().setValue(StackedBlocks.PART, i));
		helper.setBlock(radio, RadiocraftBlocks.VHF_BASE_STATION.get());
		BlockState lower = RadiocraftBlocks.J_POLE_ANTENNA.get().defaultBlockState()
				.setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.LOWER).setValue(DoubleVHFAntennaBlock.FACING, Direction.NORTH);
		helper.setBlock(jPole, lower);
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		RadiocraftBlocks.J_POLE_ANTENNA.get().setPlacedBy(helper.getLevel(), helper.absolutePos(jPole), lower, player,
				new ItemStack(RadiocraftBlocks.J_POLE_ANTENNA.get()));
		helper.setBlock(jPole.above(3), RadiocraftBlocks.SOLAR_PANEL.get());
		float hz = 144_200_000.0F;
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> GameTestUtil.placeWire(helper, radio.south(), RadiocraftBlocks.COAX_WIRE.get()))
				.thenIdle(3)
				.thenExecute(() -> {
					GameTestUtil.blockEntity(helper, module, CommercialRadioModuleBlockEntity.class)
							.configure(StationProgram.Kind.DF_BEEPER, Band.getBand("2m"), hz);
					RadioBlockEntity station = radio(helper, radio);
					station.setFrequency(hz);
					if(!GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).isPowered)
						station.toggle();
				})
				.thenWaitUntil(() -> {
					var antennas = GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).getAntennas();
					helper.assertTrue(radio(helper, radio).getLastSignalGameTime() >= 0, "the player's J-pole station never heard the module: antennas "
							+ antennas.size() + (antennas.isEmpty() ? "" : ", recognised " + (antennas.getFirst().getAntenna() != null)
							+ ", on the VHF network " + (antennas.getFirst().getAntenna() != null && AntennaNetworkManager.getNetwork(AntennaNetworkManager.VHF_ID)
							.allAntennas().contains(antennas.getFirst().getAntenna()))
							+ ", radios on it " + antennas.getFirst().getRadios().size()));
				})
				.thenSucceed();
	}

	/**
	 * After a restart (or a world left and joined again) the networks are rebuilt from the block entities as they load
	 * from the save, which goes through the block entity type rather than the block: A's J-pole is loaded that way into a
	 * fresh network object. It must come back on the VHF antenna network and A must still hear B.
	 */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
	public static void hearsAfterTheAntennaLoadsFromTheSave(GameTestHelper helper) {
		stations(helper);
		long[] mark = new long[1];
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> tune(helper, 146_820_000.0F))
				.thenWaitUntil(() -> assertAHearsB(helper, -1L, "at first"))
				.thenExecute(() -> {
					ServerLevel level = helper.getLevel();
					BlockPos pos = helper.absolutePos(A.south(2));
					AntennaBlockEntity old = GameTestUtil.blockEntity(helper, A.south(2), AntennaBlockEntity.class);
					var tag = old.saveWithFullMetadata(level.registryAccess());
					com.arrl.radiocraft.common.capabilities.BENetworksCapability.get(level).removeObject(pos); // As if the world were loaded afresh.
					level.removeBlockEntity(pos);
					var fresh = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, level.getBlockState(pos), tag, level.registryAccess());
					helper.assertTrue(fresh instanceof AntennaBlockEntity, "the J-pole did not load back as an antenna");
					level.setBlockEntity(fresh);
					fresh.onLoad();
					AntennaNetworkObject object = GameTestUtil.networkObject(helper, A.south(2), AntennaNetworkObject.class);
					helper.assertTrue(AntennaNetworkManager.VHF_ID.equals(object.getNetworkId()), "the reloaded J-pole is on the " + object.getNetworkId() + " network, not VHF");
					mark[0] = now(helper);
				})
				.thenWaitUntil(() -> assertAHearsB(helper, mark[0], "after its J-pole loaded from the save"))
				.thenSucceed();
	}
}
