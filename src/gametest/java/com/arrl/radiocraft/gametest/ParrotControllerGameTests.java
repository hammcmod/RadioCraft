package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.ParrotControllerBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioBlockEntity;
import com.arrl.radiocraft.common.blocks.radios.ParrotControllerBlock;
import com.arrl.radiocraft.common.blocks.radios.ParrotControllerBlock.LinkSide;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.arrl.radiocraft.gametest.TransmissionGameTests.*;

/**
 * The parrot controller block driving radio B of the two-station layout in {@link TransmissionGameTests}. The
 * controller sits east of radio B, so its link should point west. Frequencies continue the unique-per-test scheme
 * from that class.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ParrotControllerGameTests {

    private static final BlockPos CONTROLLER = RADIO_B.east();
    private static final int F_LINK = 215;
    private static final int F_ECHO = 230;
    private static final int F_REMOVE = 245;
    private static final int F_COMPARATOR = 260;

    private static ParrotControllerBlockEntity controller(GameTestHelper helper) {
        return GameTestUtil.blockEntity(helper, CONTROLLER, ParrotControllerBlockEntity.class);
    }

    private static BlockState controllerState(GameTestHelper helper) {
        return helper.getBlockState(CONTROLLER);
    }

    /** Places the controller the way a player would: with the link worked out from its neighbours. */
    private static void placeController(GameTestHelper helper) {
        LinkSide link = ParrotControllerBlock.findRadio(helper.getLevel(), helper.absolutePos(CONTROLLER), LinkSide.NONE);
        helper.setBlock(CONTROLLER, RadiocraftBlocks.PARROT_CONTROLLER.get().defaultBlockState().setValue(ParrotControllerBlock.LINK, link));
    }

    /** Placing the controller next to a radio links it, and the radio starts parroting with the controller's delay. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void controllerLinksToAdjacentRadio(GameTestHelper helper) {
        stationsUp(helper)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_B, F_LINK);
                    placeController(helper);
                    helper.assertTrue(controllerState(helper).getValue(ParrotControllerBlock.LINK) == LinkSide.WEST, "Controller did not find the radio to its west");
                })
                .thenWaitUntil(() -> helper.assertTrue(radio(helper, RADIO_B).getParrotDelay() == ParrotControllerBlockEntity.DEFAULT_DELAY_TICKS,
                        "Radio B parrot delay is " + radio(helper, RADIO_B).getParrotDelay()))
                .thenExecute(() -> {
                    controller(helper).cycleDelay(); // 2s -> 3s
                    helper.assertTrue(controller(helper).getDelayTicks() == 60, "Delay after one cycle is " + controller(helper).getDelayTicks());
                })
                .thenWaitUntil(() -> helper.assertTrue(radio(helper, RADIO_B).getParrotDelay() == 60, "Radio did not follow the new delay"))
                .thenSucceed();
    }

    /** The linked radio echoes A's voice after the controller's delay, and the LEDs follow receive and echo. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void controllerEchoesVoiceAndLightsLeds(GameTestHelper helper) {
        long[] spokeAt = new long[1];
        stationsUp(helper)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_A, F_ECHO);
                    setupSsb(helper, RADIO_B, F_ECHO);
                    placeController(helper);
                })
                .thenWaitUntil(() -> helper.assertTrue(radio(helper, RADIO_B).isParrot(), "Radio B is not a parrot yet"))
                .thenExecute(() -> {
                    helper.assertFalse(controllerState(helper).getValue(ParrotControllerBlock.RECEIVING), "Receive LED on before anything was received");
                    spokeAt[0] = helper.getLevel().getGameTime();
                    speakInto(helper, RADIO_A);
                    assertHeard(helper, RADIO_B, EXPECTED_VOICE_STRENGTH);
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(controllerState(helper).getValue(ParrotControllerBlock.RECEIVING), "Receive LED did not light");
                    helper.assertFalse(controllerState(helper).getValue(ParrotControllerBlock.TRANSMITTING), "Transmit LED lit before the echo");
                })
                .thenWaitUntil(() -> helper.assertTrue(radio(helper, RADIO_A).getLastSignalGameTime() >= 0, "A has not heard the echo yet"))
                .thenExecute(() -> {
                    HFRadioBlockEntity a = radio(helper, RADIO_A);
                    long waited = a.getLastSignalGameTime() - spokeAt[0];
                    helper.assertTrue(waited >= ParrotControllerBlockEntity.DEFAULT_DELAY_TICKS, "Echo came back after " + waited + " ticks");
                    helper.assertTrue(Math.abs(a.getLastSignalStrength() - EXPECTED_VOICE_STRENGTH) < TOLERANCE, "Echo strength " + a.getLastSignalStrength());
                })
                .thenExecuteAfter(2, () -> helper.assertTrue(controllerState(helper).getValue(ParrotControllerBlock.TRANSMITTING), "Transmit LED did not light for the echo"))
                .thenExecuteAfter(ParrotControllerBlockEntity.LED_HOLD_TICKS + 2, () ->
                        helper.assertFalse(controllerState(helper).getValue(ParrotControllerBlock.RECEIVING), "Receive LED stayed on"))
                // The transmit LED stays on through the courtesy beep after the echo, then goes out.
                .thenWaitUntil(() -> {
                    helper.assertFalse(controllerState(helper).getValue(ParrotControllerBlock.TRANSMITTING), "Transmit LED stayed on");
                })
                .thenSucceed();
    }

    /** A comparator against the controller reads the delay in seconds and follows it when it is cycled. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void comparatorReadsDelayInSeconds(GameTestHelper helper) {
        BlockPos comparator = CONTROLLER.east();
        stationsUp(helper)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_B, F_COMPARATOR);
                    ComparatorGameTests.placeComparator(helper, comparator, Direction.WEST); // Before the controller, so placing it updates the comparator.
                    placeController(helper);
                    helper.assertTrue(controller(helper).getComparatorOutput() == 2, "Default delay should read 2, got " + controller(helper).getComparatorOutput());
                })
                .thenWaitUntil(() -> helper.assertTrue(ComparatorGameTests.comparatorOutput(helper, comparator) == 2, "Comparator shows " + ComparatorGameTests.comparatorOutput(helper, comparator) + " for a 2 s delay"))
                .thenExecute(() -> controller(helper).cycleDelay()) // 2s -> 3s
                .thenWaitUntil(() -> helper.assertTrue(ComparatorGameTests.comparatorOutput(helper, comparator) == 3, "Comparator did not follow the delay to 3: " + ComparatorGameTests.comparatorOutput(helper, comparator)))
                .thenSucceed();
    }

    /** Breaking the controller stops the radio parroting. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void removingControllerReleasesRadio(GameTestHelper helper) {
        stationsUp(helper)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_B, F_REMOVE);
                    placeController(helper);
                })
                .thenWaitUntil(() -> helper.assertTrue(radio(helper, RADIO_B).isParrot(), "Radio B is not a parrot yet"))
                .thenExecute(() -> helper.setBlock(CONTROLLER, Blocks.AIR))
                .thenExecuteAfter(2, () -> helper.assertFalse(radio(helper, RADIO_B).isParrot(), "Radio B still parrots after its controller was broken"))
                .thenSucceed();
    }
}
