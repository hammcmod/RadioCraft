package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.AntennaBlockEntity;
import com.arrl.radiocraft.common.blockentities.CommercialRadioModuleBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.blocks.StackedBlocks;
import com.arrl.radiocraft.common.blocks.antennas.DoubleVHFAntennaBlock;
import com.arrl.radiocraft.common.capabilities.BENetworksCapability;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.common.radio.voice.StationProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The commercial VHF antennas (Ringo, folded dipole), placed as a player places them, hear a 2m station, and are still
 * on the VHF antenna network, hearing it, after their block entity loads back from the save.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CommercialAntennaGameTests {

	private static final String BATCH = "commercial_antennas";

	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
	public static void ringoHearsAndSurvivesAReload(GameTestHelper helper) {
		hearsAndSurvivesAReload(helper, RadiocraftBlocks.RINGO_ANTENNA.get(), "ringo");
	}

	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
	public static void foldedDipoleHearsAndSurvivesAReload(GameTestHelper helper) {
		hearsAndSurvivesAReload(helper, RadiocraftBlocks.FOLDED_DIPOLE_ANTENNA.get(), "folded_dipole");
	}

	private static void hearsAndSurvivesAReload(GameTestHelper helper, DoubleVHFAntennaBlock block, String typeId) {
		BlockPos module = new BlockPos(2, 2, 12), radio = new BlockPos(12, 2, 2), antenna = radio.south(2);
		for(int i = 0; i < StackedBlocks.HEIGHT; i++)
			helper.setBlock(module.above(i), RadiocraftBlocks.COMMERCIAL_RADIO_MODULE.get().defaultBlockState().setValue(StackedBlocks.PART, i));
		helper.setBlock(radio, RadiocraftBlocks.VHF_BASE_STATION.get());
		BlockState lower = block.defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.LOWER)
				.setValue(DoubleVHFAntennaBlock.FACING, Direction.NORTH);
		helper.setBlock(antenna, lower);
		block.setPlacedBy(helper.getLevel(), helper.absolutePos(antenna), lower, helper.makeMockPlayer(GameType.SURVIVAL), new ItemStack(block));
		float hz = 144_200_000.0F;
		long[] mark = { -1L };
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> GameTestUtil.placeWire(helper, radio.south(), RadiocraftBlocks.COAX_WIRE.get()))
				.thenIdle(3)
				.thenExecute(() -> {
					GameTestUtil.blockEntity(helper, module, CommercialRadioModuleBlockEntity.class).configure(StationProgram.Kind.DF_BEEPER, Band.getBand("2m"), hz);
					RadioBlockEntity station = GameTestUtil.blockEntity(helper, radio, RadioBlockEntity.class);
					station.setFrequency(hz);
					if(!GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).isPowered)
						station.toggle();
				})
				.thenWaitUntil(() -> {
					var antennas = GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).getAntennas();
					helper.assertTrue(!antennas.isEmpty() && antennas.getFirst().getAntenna() != null
							&& antennas.getFirst().getAntenna().getType().getId().getPath().equals(typeId), "the station's antenna is not a " + typeId);
					helper.assertTrue(GameTestUtil.blockEntity(helper, radio, RadioBlockEntity.class).getLastSignalGameTime() >= 0,
							"the " + typeId + " station never heard the module");
				})
				.thenExecute(() -> {
					ServerLevel level = helper.getLevel();
					BlockPos pos = helper.absolutePos(antenna);
					AntennaBlockEntity old = GameTestUtil.blockEntity(helper, antenna, AntennaBlockEntity.class);
					var tag = old.saveWithFullMetadata(level.registryAccess());
					BENetworksCapability.get(level).removeObject(pos); // As if the world were loaded afresh.
					level.removeBlockEntity(pos);
					BlockEntity fresh = BlockEntity.loadStatic(pos, level.getBlockState(pos), tag, level.registryAccess());
					helper.assertTrue(fresh instanceof AntennaBlockEntity, "the " + typeId + " did not load back as an antenna");
					level.setBlockEntity(fresh);
					fresh.onLoad();
					AntennaNetworkObject object = GameTestUtil.networkObject(helper, antenna, AntennaNetworkObject.class);
					helper.assertTrue(AntennaNetworkManager.VHF_ID.equals(object.getNetworkId()), "the reloaded " + typeId + " is on the "
							+ object.getNetworkId() + " network, not VHF");
					mark[0] = level.getGameTime();
				})
				.thenWaitUntil(() -> helper.assertTrue(GameTestUtil.blockEntity(helper, radio, RadioBlockEntity.class).getLastSignalGameTime() > mark[0],
						"the " + typeId + " station went deaf after the antenna loaded from the save"))
				.thenSucceed();
	}
}
