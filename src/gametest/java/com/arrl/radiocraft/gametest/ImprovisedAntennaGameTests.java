package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftAntennaTypes;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Any antenna beats no antenna: wires on a balun that are not a proper antenna still work, as an "improvised" antenna
 * with a worse SWR the further the build is from an ideal shape.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ImprovisedAntennaGameTests {

    private static final BlockPos RADIO = new BlockPos(8, 3, 3);
    private static final BlockPos BALUN = new BlockPos(8, 3, 8);

    private static void coax(GameTestHelper helper) {
        for (int z = RADIO.getZ() + 1; z < BALUN.getZ(); z++)
            GameTestUtil.placeWire(helper, new BlockPos(8, 3, z), RadiocraftBlocks.COAX_WIRE.get());
    }

    private static StaticAntenna<?> antennaAt(GameTestHelper helper) {
        return GameTestUtil.networkObject(helper, BALUN, AntennaNetworkObject.class).getAntenna();
    }

    /**
     * The playtest build: a 20m dipole on a 1:1 balun with one arm dropping a block to its pole. Not a flat dipole, not an
     * inverted V, but it works with an SWR close to a dipole's, and the radio uses it.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void dipoleWithOneArmDippingStillWorks(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        helper.setBlock(BALUN, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, BALUN, BALUN.west(5));
        GameTestUtil.wire(helper, BALUN, BALUN.east(5).below());
        coax(helper);
        helper.succeedWhen(() -> {
            StaticAntenna<?> antenna = antennaAt(helper);
            helper.assertTrue(antenna != null, "No antenna recognized");
            helper.assertTrue(antenna.type == RadiocraftAntennaTypes.IMPROVISED, "Expected improvised, got " + antenna.type.getId());
            RadioBlockEntity radio = GameTestUtil.blockEntity(helper, RADIO, RadioBlockEntity.class);
            List<AntennaNetworkObject> antennas = GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class).getAntennas();
            helper.assertTrue(antennas.size() == 1, "Radio sees " + antennas.size() + " antennas");
            double swr = antennas.getFirst().getSWR(radio.getFrequency());
            helper.assertTrue(swr > 1.0D && swr < 2.0D, "A nearly-dipole should be a decent match, SWR " + swr);
        });
    }

    /** A proper flat dipole is still a dipole: the improvised type only catches what nothing else recognizes. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void properDipoleIsNotImprovised(GameTestHelper helper) {
        helper.setBlock(BALUN, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, BALUN, BALUN.west(5));
        GameTestUtil.wire(helper, BALUN, BALUN.east(5));
        helper.succeedWhen(() -> {
            StaticAntenna<?> antenna = antennaAt(helper);
            helper.assertTrue(antenna != null && antenna.type == RadiocraftAntennaTypes.DIPOLE, "Expected a dipole, got " + (antenna == null ? "none" : antenna.type.getId()));
        });
    }

    /** Three wires every which way on a 4:1 balun: a poor match, but still an antenna. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void tangleIsAPoorButWorkingAntenna(GameTestHelper helper) {
        helper.setBlock(BALUN, RadiocraftBlocks.BALUN_FOUR_TO_ONE.get());
        GameTestUtil.wire(helper, BALUN, BALUN.west(2));
        GameTestUtil.wire(helper, BALUN, BALUN.north(3).above(2));
        GameTestUtil.wire(helper, BALUN, BALUN.south(7).east(1));
        helper.succeedWhen(() -> {
            StaticAntenna<?> antenna = antennaAt(helper);
            helper.assertTrue(antenna != null && antenna.type == RadiocraftAntennaTypes.IMPROVISED, "Expected improvised, got " + (antenna == null ? "none" : antenna.type.getId()));
            double swr = antenna.getSWR(14_175_000.0F);
            helper.assertTrue(swr > 4.0D, "A tangle should be a poor match, SWR " + swr);
        });
    }
}
