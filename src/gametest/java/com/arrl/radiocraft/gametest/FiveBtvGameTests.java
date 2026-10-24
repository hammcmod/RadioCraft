package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blocks.antennas.FiveBtvAntennaBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.antenna.types.FiveBtvAntennaType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** The Hustler 5-BTV (maintainer's model, 2026-10-23): seven blocks tall, broken as one, matched on HF and VHF. */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FiveBtvGameTests {

    private static final String BATCH = "five_btv";

    private static void place(GameTestHelper helper, BlockPos base) {
        for(int i = 0; i < FiveBtvAntennaBlock.HEIGHT; i++)
            helper.setBlock(base.above(i), RadiocraftBlocks.FIVE_BTV_ANTENNA.get().defaultBlockState().setValue(FiveBtvAntennaBlock.SEGMENT, i));
    }

    private static List<AntennaNetworkObject> antennas(GameTestHelper helper, BlockPos radioPos) {
        return GameTestUtil.networkObject(helper, radioPos, RadioNetworkObject.class).getAntennas();
    }

    /** On coax to an HF radio and a VHF base station, each sees the 5-BTV with SWR 1.3. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = BATCH)
    public static void matchedOnHfAndVhf(GameTestHelper helper) {
        BlockPos hf = new BlockPos(3, 2, 3), vhf = new BlockPos(10, 2, 3);
        for(BlockPos radio : List.of(hf, vhf)) {
            helper.setBlock(radio, radio == hf ? RadiocraftBlocks.HF_RADIO_20M.get() : RadiocraftBlocks.VHF_BASE_STATION.get());
            GameTestUtil.placeWire(helper, radio.south(), RadiocraftBlocks.COAX_WIRE.get());
            place(helper, radio.south(2));
        }
        helper.startSequence()
                .thenWaitUntil(() -> {
                    for(BlockPos radio : List.of(hf, vhf)) {
                        List<AntennaNetworkObject> found = antennas(helper, radio);
                        helper.assertTrue(found.size() == 1 && found.getFirst().getAntenna() != null, "The radio at " + radio.toShortString() + " does not see the 5-BTV");
                    }
                })
                .thenExecute(() -> {
                    for(float hz : new float[] { 3_600_000.0F, 7_100_000.0F, 14_200_000.0F, 28_400_000.0F })
                        helper.assertTrue(antennas(helper, hf).getFirst().getSWR(hz) == FiveBtvAntennaType.SWR, "SWR " + antennas(helper, hf).getFirst().getSWR(hz) + " at " + hz);
                    helper.assertTrue(antennas(helper, vhf).getFirst().getSWR(146_520_000.0F) == FiveBtvAntennaType.SWR, "VHF SWR " + antennas(helper, vhf).getFirst().getSWR(146_520_000.0F));
                })
                .thenSucceed();
    }

    /** Breaking a segment in the middle brings the whole antenna down and drops it once. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40, batch = BATCH)
    public static void breaksAsOneAndDropsOnce(GameTestHelper helper) {
        BlockPos base = new BlockPos(3, 2, 3);
        Block block = RadiocraftBlocks.FIVE_BTV_ANTENNA.get();
        place(helper, base);
        helper.getLevel().destroyBlock(helper.absolutePos(base.above(3)), true);
        helper.runAfterDelay(2, () -> {
            for(int i = 0; i < FiveBtvAntennaBlock.HEIGHT; i++)
                helper.assertBlockNotPresent(block, base.above(i));
            int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(base)).inflate(2.0D, 8.0D, 2.0D))
                    .stream().filter(item -> item.getItem().is(block.asItem())).mapToInt(item -> item.getItem().getCount()).sum();
            helper.assertTrue(dropped == 1, "Breaking the 5-BTV dropped " + dropped);
            helper.succeed();
        });
    }
}
