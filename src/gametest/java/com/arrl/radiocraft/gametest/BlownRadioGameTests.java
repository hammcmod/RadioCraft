package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blocks.radios.RadioBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.radio.crystal.CrystalBand;
import com.arrl.radiocraft.common.radio.crystal.CrystalQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.arrl.radiocraft.gametest.TransmissionGameTests.*;

/**
 * Blown radios (maintainer, 2026-10-23): transmitting into a bad SWR can burn out the transmitter. A blown radio still
 * receives but sends nothing, and broken it gives back its crafting parts minus the crystal. A poor-crystal radio may
 * go up like TNT instead. A batch of its own, because the chances are forced to certain here and the blow chance is 0
 * for every other test.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class BlownRadioGameTests {

    private static final String BATCH = "blown_radio";
    private static final int F_SENDS_NOTHING = 20;

    @BeforeBatch(batch = BATCH)
    public static void blowOnEveryChance(ServerLevel level) {
        RadiocraftServerConfig.RADIO_BLOW_CHANCE.set(1.0D);
        RadiocraftServerConfig.RADIO_EXPLODE_CHANCE.set(1.0D);
    }

    @AfterBatch(batch = BATCH)
    public static void restoreChances(ServerLevel level) {
        RadiocraftServerConfig.RADIO_BLOW_CHANCE.set(0.0D);
        RadiocraftServerConfig.RADIO_EXPLODE_CHANCE.set(RadiocraftServerConfig.RADIO_EXPLODE_CHANCE.getDefault());
    }

    private static boolean blown(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(RadioBlock.BLOWN);
    }

    /** Keyed into a 2+2 dipole on 20m (well past SWR 3) the radio blows, and its RF stops reaching nearby redstone. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 600, batch = BATCH)
    public static void badSwrBlowsTheRadio(GameTestHelper helper) {
        TransmitInterferenceGameTests.buildStationWithArms(helper, RADIO_A, BALUN_A, 2);
        helper.runAfterDelay(5, () -> setupSsb(helper, RADIO_A, 0));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(radio(helper, RADIO_A).getAntennaSWR() >= RadiocraftServerConfig.RADIO_BLOW_SWR.get(),
                        "SWR is " + radio(helper, RADIO_A).getAntennaSWR()))
                .thenExecute(() -> radio(helper, RADIO_A).setPTTDown(true))
                .thenWaitUntil(() -> helper.assertTrue(blown(helper, RADIO_A), "Radio has not blown yet"))
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(radio(helper, RADIO_A).getTransmitTargets() == null, "A blown radio still disturbs redstone");
                    radio(helper, RADIO_A).setPTTDown(false);
                })
                .thenSucceed();
    }

    /** A matched station keyed with the blow chance at certain never blows: SWR 1 is below the threshold. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
    public static void matchedStationNeverBlows(GameTestHelper helper) {
        buildStation(helper, RADIO_A, BALUN_A);
        helper.runAfterDelay(5, () -> setupSsb(helper, RADIO_A, 0));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(radio(helper, RADIO_A).getAntennaSWR() == 1.0D, "SWR is " + radio(helper, RADIO_A).getAntennaSWR()))
                .thenExecute(() -> radio(helper, RADIO_A).setPTTDown(true))
                .thenIdle(100)
                .thenExecute(() -> {
                    helper.assertFalse(blown(helper, RADIO_A), "A matched radio blew");
                    radio(helper, RADIO_A).setPTTDown(false);
                })
                .thenSucceed();
    }

    /** Speaking into a blown radio puts nothing on the air, but it still hears the other station. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
    public static void blownRadioSendsNothingButStillHears(GameTestHelper helper) {
        stationsUp(helper)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_A, F_SENDS_NOTHING);
                    setupSsb(helper, RADIO_B, F_SENDS_NOTHING);
                    radio(helper, RADIO_A).blow(helper.getLevel());
                    helper.assertTrue(blown(helper, RADIO_A), "blow() did not blow the radio");
                    speakInto(helper, RADIO_A);
                    assertHeardNothing(helper, RADIO_B);
                    speakInto(helper, RADIO_B);
                    assertHeard(helper, RADIO_A, EXPECTED_VOICE_STRENGTH);
                })
                .thenSucceed();
    }

    /**
     * Broken, a blown 20m radio gives back its board, mic, speaker, tinted glass, wire and three iron, but not its
     * encased crystal; a working one still gives itself. A good-crystal radio never explodes, even at chance 1.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40, batch = BATCH)
    public static void blownRadioDropsItsPartsButNotItsCrystal(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 2, 5);
        helper.setBlock(pos, RadiocraftBlocks.HF_RADIO_20M.get());
        helper.runAfterDelay(2, () -> {
            ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
            BlockPos abs = helper.absolutePos(pos);
            List<ItemStack> whole = Block.getDrops(helper.getBlockState(pos), helper.getLevel(), abs, null, null, pickaxe);
            helper.assertTrue(whole.size() == 1 && whole.get(0).is(RadiocraftItems.HF_RADIO_20M.get()), "A working radio drops " + whole);

            radio(helper, pos).blow(helper.getLevel());
            helper.assertBlockPresent(RadiocraftBlocks.HF_RADIO_20M.get(), pos);
            helper.assertTrue(blown(helper, pos), "Good radio did not blow");

            Map<Item, Integer> drops = Block.getDrops(helper.getBlockState(pos), helper.getLevel(), abs, null, null, pickaxe).stream()
                    .collect(Collectors.toMap(ItemStack::getItem, ItemStack::getCount, Integer::sum));
            Map<Item, Integer> expected = new HashMap<>();
            for(Item part : List.of(RadiocraftItems.HF_CIRCUIT_BOARD.get(), RadiocraftItems.HAND_MICROPHONE.get(), RadiocraftItems.RADIO_SPEAKER.get(),
                    RadiocraftItems.WIRE.get(), Items.TINTED_GLASS))
                expected.put(part, 1);
            expected.put(Items.IRON_INGOT, 3);
            helper.assertTrue(drops.equals(expected), "A blown radio drops " + drops + ", expected " + expected);
            helper.assertFalse(drops.containsKey(RadiocraftItems.ENCASED_CRYSTALS.get(CrystalBand.BAND_20M).get(CrystalQuality.GOOD).get()), "The crystal came back");
            helper.succeed();
        });
    }

    /** A poor-crystal radio that blows with the explode chance at 1 goes up like TNT and is gone. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40, batch = BATCH)
    public static void poorRadioCanExplode(GameTestHelper helper) {
        BlockPos pos = new BlockPos(8, 2, 8);
        helper.setBlock(pos, RadiocraftBlocks.HF_RADIO_20M_VARIANTS.poor().get());
        helper.runAfterDelay(2, () -> {
            radio(helper, pos).blow(helper.getLevel());
            helper.assertBlockPresent(Blocks.AIR, pos);
            helper.succeed();
        });
    }
}
