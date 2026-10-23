package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blocks.antennas.DoubleVHFAntennaBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.common.radio.antenna.types.IonosondeAntennaType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * The ionosonde antenna (maintainer's model, 2026-10-23): two blocks tall, broken as one, and on a radio's coax a poor
 * match on every band, worst on HF, on the antenna network of the radio's band.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class IonosondeGameTests {

    private static final String BATCH = "ionosonde";

    private static List<AntennaNetworkObject> antennas(GameTestHelper helper, BlockPos radioPos) {
        return GameTestUtil.networkObject(helper, radioPos, RadioNetworkObject.class).getAntennas();
    }

    /** The radio, coax south of it, and the ionosonde south of that. */
    private static void station(GameTestHelper helper, BlockPos radio, Block radioBlock) {
        helper.setBlock(radio, radioBlock);
        GameTestUtil.placeWire(helper, radio.south(), RadiocraftBlocks.COAX_WIRE.get());
        BlockPos lower = radio.south(2);
        helper.setBlock(lower, RadiocraftBlocks.IONOSONDE.get().defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(lower.above(), RadiocraftBlocks.IONOSONDE.get().defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** SWR 3 on a VHF radio and 8 on an HF one, each ionosonde on its radio's antenna network. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = BATCH)
    public static void poorMatchWorstOnHf(GameTestHelper helper) {
        BlockPos vhf = new BlockPos(3, 2, 3);
        BlockPos hf = new BlockPos(10, 2, 3);
        station(helper, vhf, RadiocraftBlocks.VHF_BASE_STATION.get());
        station(helper, hf, RadiocraftBlocks.HF_RADIO_20M.get());
        helper.startSequence()
                .thenWaitUntil(() -> {
                    for(BlockPos radio : List.of(vhf, hf)) {
                        List<AntennaNetworkObject> found = antennas(helper, radio);
                        helper.assertTrue(found.size() == 1 && found.getFirst().getAntenna() != null, "The radio at " + radio.toShortString() + " does not see the ionosonde");
                    }
                })
                .thenWaitUntil(() -> helper.assertTrue(AntennaNetworkManager.VHF_ID.equals(antennas(helper, vhf).getFirst().getNetworkId()),
                        "The base station's ionosonde is not on the VHF network"))
                .thenExecute(() -> {
                    double vhfSwr = antennas(helper, vhf).getFirst().getSWR(146_520_000.0F);
                    double hfSwr = antennas(helper, hf).getFirst().getSWR(14_230_000.0F);
                    helper.assertTrue(vhfSwr == IonosondeAntennaType.VHF_SWR, "VHF SWR " + vhfSwr);
                    helper.assertTrue(hfSwr == IonosondeAntennaType.HF_SWR, "HF SWR " + hfSwr);
                    helper.assertTrue(AntennaNetworkManager.HF_ID.equals(antennas(helper, hf).getFirst().getNetworkId()), "The HF radio's ionosonde left the HF network");
                })
                .thenSucceed();
    }

    /** Breaking the top half takes the bottom with it. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20, batch = BATCH)
    public static void breaksAsOneBlock(GameTestHelper helper) {
        BlockPos lower = new BlockPos(3, 2, 3);
        helper.setBlock(lower, RadiocraftBlocks.IONOSONDE.get().defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(lower.above(), RadiocraftBlocks.IONOSONDE.get().defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.UPPER));
        helper.startSequence()
                .thenExecute(() -> helper.setBlock(lower.above(), Blocks.AIR))
                .thenExecute(() -> helper.assertBlockNotPresent(RadiocraftBlocks.IONOSONDE.get(), lower))
                .thenSucceed();
    }
}
