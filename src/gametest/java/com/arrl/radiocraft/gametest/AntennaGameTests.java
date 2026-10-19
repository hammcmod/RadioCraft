package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.AntennaTypes;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.DipoleAntennaData;
import com.arrl.radiocraft.common.radio.antenna.data.EndFedAntennaData;
import com.arrl.radiocraft.common.radio.antenna.types.DipoleAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.EndFedAntennaType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Antenna structures are recognized from the blocks and wires around a balun.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class AntennaGameTests {

    private static final BlockPos CENTER = new BlockPos(8, 2, 8);
    private static final float TWENTY_M = 14_000_000;

    private static StaticAntenna<?> match(GameTestHelper helper, BlockPos pos) {
        IAntenna antenna = AntennaTypes.match(helper.getLevel(), helper.absolutePos(pos));
        if (!(antenna instanceof StaticAntenna<?> staticAntenna)) {
            helper.fail("No antenna recognized", pos);
            return null;
        }
        return staticAntenna;
    }

    @GameTest(template = GameTestUtil.EMPTY)
    public static void dipoleIsRecognized(GameTestHelper helper) {
        helper.setBlock(CENTER, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, CENTER, CENTER.west(5));
        GameTestUtil.wire(helper, CENTER, CENTER.east(5));

        helper.succeedWhen(() -> {
            StaticAntenna<?> antenna = match(helper, CENTER);
            helper.assertTrue(antenna.type instanceof DipoleAntennaType, "Expected dipole, got " + antenna.type.getId());
            DipoleAntennaData data = (DipoleAntennaData) antenna.data;
            helper.assertTrue(data.getArmLength1() == 5 && data.getArmLength2() == 5,
                    "Arm lengths " + data.getArmLength1() + "/" + data.getArmLength2());
            helper.assertTrue(antenna.getSWR(TWENTY_M) == 1.0D, "20m SWR was " + antenna.getSWR(TWENTY_M));
        });
    }

    @GameTest(template = GameTestUtil.EMPTY)
    public static void endFedIsRecognized(GameTestHelper helper) {
        helper.setBlock(CENTER, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, CENTER, CENTER.north(6));

        helper.succeedWhen(() -> {
            StaticAntenna<?> antenna = match(helper, CENTER);
            helper.assertTrue(antenna.type instanceof EndFedAntennaType, "Expected end fed, got " + antenna.type.getId());
            helper.assertTrue(((EndFedAntennaData) antenna.data).getLength() == 6, "Wrong length");
            helper.assertTrue(antenna.getSWR(TWENTY_M) == 1.5D, "20m SWR was " + antenna.getSWR(TWENTY_M));
        });
    }

    @GameTest(template = GameTestUtil.EMPTY)
    public static void bentDipoleIsNotRecognized(GameTestHelper helper) {
        helper.setBlock(CENTER, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, CENTER, CENTER.west(5));
        GameTestUtil.wire(helper, CENTER, CENTER.north(5)); // 90 degrees, not opposite

        helper.runAtTickTime(5, () -> {
            IAntenna antenna = AntennaTypes.match(helper.getLevel(), helper.absolutePos(CENTER));
            if (antenna instanceof StaticAntenna<?> s && s.type instanceof DipoleAntennaType)
                helper.fail("Perpendicular arms should not make a dipole", CENTER);
            helper.succeed();
        });
    }
}
