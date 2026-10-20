package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.radio.rfi.LightningStatic;
import com.arrl.radiocraft.common.radio.rfi.RedstoneInterference;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.arrl.radiocraft.gametest.TransmissionGameTests.*;

/**
 * Lightning crashes on a powered station. A strike is heard hundreds of blocks away, so a bolt in one plot would reach
 * every radio in the neighbouring plots and upset the tests that expect a quiet radio: this runs in its own batch, and
 * everything is one sequence so no two bolts are in the air at once. Uses station A; nothing transmits.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class LightningStaticGameTests {

    private static final BlockPos NEAR = RADIO_A.east(2);
    private static final BlockPos FAR = new BlockPos(1, 2, 14); // The far corner of the plot, about 15 blocks from the radio.

    /** Drops a (visual only, so nothing catches fire) bolt at {@code pos}, the way the weather or a trident does. */
    private static Vec3 strike(GameTestHelper helper, BlockPos pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(helper.getLevel());
        helper.assertTrue(bolt != null, "Could not create a lightning bolt");
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(pos));
        bolt.moveTo(at);
        bolt.setVisualOnly(true);
        helper.getLevel().addFreshEntity(bolt);
        return at;
    }

    /** What a strike at {@code at} adds to radio A before any decay. */
    private static double expectedPop(GameTestHelper helper, Vec3 at) {
        double distance = Math.sqrt(helper.absolutePos(RADIO_A).distToCenterSqr(at));
        return RadiocraftServerConfig.LIGHTNING_STATIC_STRENGTH.get()
                * LightningStatic.falloff(distance, RadiocraftServerConfig.LIGHTNING_STATIC_RANGE.get())
                * RedstoneInterference.bandFactor(radio(helper, RADIO_A).getBand());
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400, batch = "lightning")
    public static void lightningCrashesOnPoweredRadioAndFadeWithDistance(GameTestHelper helper) {
        buildStation(helper, RADIO_A, BALUN_A);
        helper.runAfterDelay(5, () -> setupSsb(helper, RADIO_A, 0));
        double[] near = new double[1];
        Vec3[] at = new Vec3[1];

        // A strike two blocks away: a big pop, then gone within a second or two.
        helper.runAtTickTime(30, () -> {
            helper.assertTrue(radio(helper, RADIO_A).getInterference() == 0.0D, "Interference before any lightning");
            at[0] = strike(helper, NEAR);
        });
        helper.runAtTickTime(32, () -> {
            near[0] = radio(helper, RADIO_A).getInterference();
            double expected = expectedPop(helper, at[0]);
            helper.assertTrue(near[0] > expected * 0.6D && near[0] <= expected, "Crash from a near strike is " + near[0] + ", expected about " + expected);
        });
        helper.runAtTickTime(32 + 60, () -> helper.assertTrue(radio(helper, RADIO_A).getInterference() == 0.0D, "Crash did not die away: " + radio(helper, RADIO_A).getInterference()));

        // The same strike at the far side of the plot is heard, but less.
        helper.runAtTickTime(100, () -> at[0] = strike(helper, FAR));
        helper.runAtTickTime(102, () -> {
            double far = radio(helper, RADIO_A).getInterference();
            helper.assertTrue(far > 0.0D, "A strike 15 blocks away should still be heard");
            helper.assertTrue(far < near[0], "Farther strike should be quieter: " + far + " vs " + near[0]);
        });

        // A radio that is switched off hears nothing.
        helper.runAtTickTime(170, () -> TransmissionGameTests.powerOff(helper, RADIO_A));
        helper.runAtTickTime(180, () -> {
            helper.assertTrue(radio(helper, RADIO_A).getInterference() == 0.0D, "Interference left over before the unpowered strike");
            strike(helper, NEAR);
        });
        helper.runAtTickTime(182, () -> {
            helper.assertTrue(radio(helper, RADIO_A).getInterference() == 0.0D, "Unpowered radio picked up a lightning crash");
            helper.succeed();
        });
    }
}
