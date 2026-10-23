package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.radio.crystal.CrystalQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Crystal quality (maintainer, 2026-10-22): each crystal radio comes as a premium (pure), plain (good) and cheap (poor)
 * block. Pure hears a little less noise, poor a little more and drifts off frequency, QRP rigs ignore their crystal,
 * the repeater is always pure, and digging sand now and then turns up a raw crystal.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CrystalGameTests {

    private static final double EPSILON = 1e-9D;

    /** Three VHF base stations side by side: the noise each hears follows its crystal. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
    public static void crystalQualitySetsNoise(GameTestHelper helper) {
        BlockPos pure = new BlockPos(2, 2, 2), good = new BlockPos(4, 2, 2), poor = new BlockPos(6, 2, 2);
        helper.setBlock(pure, RadiocraftBlocks.VHF_BASE_STATION_VARIANTS.pure().get());
        helper.setBlock(good, RadiocraftBlocks.VHF_BASE_STATION_VARIANTS.good().get());
        helper.setBlock(poor, RadiocraftBlocks.VHF_BASE_STATION_VARIANTS.poor().get());
        helper.runAfterDelay(2, () -> {
            RadioBlockEntity pureBe = radio(helper, pure), goodBe = radio(helper, good), poorBe = radio(helper, poor);
            helper.assertTrue(pureBe.getCrystalQuality() == CrystalQuality.PURE, "The premium base station is not pure");
            helper.assertTrue(goodBe.getCrystalQuality() == CrystalQuality.GOOD, "The plain base station is not good");
            helper.assertTrue(poorBe.getCrystalQuality() == CrystalQuality.POOR, "The cheap base station is not poor");
            double floor = RadiocraftServerConfig.VHF_NOISE_FLOOR.get();
            helper.assertTrue(Math.abs(goodBe.getNoiseFloor() - floor) < EPSILON, "A good crystal changed the noise: " + goodBe.getNoiseFloor());
            helper.assertTrue(Math.abs(pureBe.getNoiseFloor() - floor * RadiocraftServerConfig.CRYSTAL_PURE_NOISE_FACTOR.get()) < EPSILON,
                    "A pure crystal hears " + pureBe.getNoiseFloor() + ", not a little less than " + floor);
            helper.assertTrue(Math.abs(poorBe.getNoiseFloor() - Math.min(1.0D, floor + RadiocraftServerConfig.CRYSTAL_POOR_EXTRA_NOISE.get())) < EPSILON,
                    "A poor crystal hears " + poorBe.getNoiseFloor() + ", not a little more than " + floor);
            helper.succeed();
        });
    }

    /** Only the poor crystal drifts; its radio listens and transmits off the dial by the drift. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
    public static void poorCrystalDrifts(GameTestHelper helper) {
        BlockPos good = new BlockPos(2, 2, 2), poor = new BlockPos(5, 2, 2);
        helper.setBlock(good, RadiocraftBlocks.HF_RADIO_40M_VARIANTS.good().get());
        helper.setBlock(poor, RadiocraftBlocks.HF_RADIO_40M_VARIANTS.poor().get());
        helper.runAfterDelay(2, () -> {
            RadioBlockEntity goodBe = radio(helper, good), poorBe = radio(helper, poor);
            helper.assertTrue(goodBe.getDriftHz() == 0.0D && goodBe.getTunedFrequency() == goodBe.getFrequency(), "A good crystal drifted");
            double expected = CrystalQuality.POOR.driftHz(helper.getLevel().getGameTime(), helper.absolutePos(poor).asLong());
            helper.assertTrue(Math.abs(poorBe.getDriftHz() - expected) < EPSILON, "The poor crystal is " + poorBe.getDriftHz() + " Hz off, expected " + expected);
            helper.assertTrue(Math.abs(poorBe.getDriftHz()) <= RadiocraftServerConfig.CRYSTAL_POOR_DRIFT_HZ.get(), "The poor crystal drifted past its limit");
            helper.succeed();
        });
    }

    /** QRP rigs are built for poor crystals: no extra noise, no drift. The repeater only comes with a pure crystal. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
    public static void qrpIgnoresCrystalAndRepeaterIsPure(GameTestHelper helper) {
        BlockPos qrp = new BlockPos(2, 2, 2), repeater = new BlockPos(5, 2, 2);
        helper.setBlock(qrp, RadiocraftBlocks.QRP_RADIO_20M.get());
        helper.setBlock(repeater, RadiocraftBlocks.VHF_REPEATER.get());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(radio(helper, qrp).getCrystalEffect() == CrystalQuality.GOOD, "The QRP rig is affected by its crystal");
            helper.assertTrue(radio(helper, qrp).getDriftHz() == 0.0D, "The QRP rig drifts");
            helper.assertTrue(radio(helper, repeater).getCrystalQuality() == CrystalQuality.PURE, "The repeater is not pure");
            helper.succeed();
        });
    }

    /** With the chance turned all the way up, every sand block dug drops a raw crystal alongside the sand. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40, batch = "crystal_sand")
    public static void sandDropsCrystals(GameTestHelper helper) {
        double chance = RadiocraftServerConfig.CRYSTAL_SAND_CHANCE.get();
        RadiocraftServerConfig.CRYSTAL_SAND_CHANCE.set(1.0D);
        BlockPos sand = new BlockPos(3, 2, 3);
        helper.setBlock(sand, Blocks.SAND);
        try {
            helper.getLevel().destroyBlock(helper.absolutePos(sand), true);
        }
        finally {
            RadiocraftServerConfig.CRYSTAL_SAND_CHANCE.set(chance);
        }
        Set<Item> crystals = RadiocraftItems.CRYSTALS.values().stream().flatMap(byQuality -> byQuality.values().stream())
                .map(java.util.function.Supplier::get).collect(Collectors.toSet());
        helper.runAfterDelay(1, () -> {
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(sand)).inflate(2.0D));
            helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(Blocks.SAND.asItem())), "Digging the sand did not drop the sand");
            helper.assertTrue(drops.stream().anyMatch(item -> crystals.contains(item.getItem().getItem())), "Digging the sand did not drop a crystal");
            helper.succeed();
        });
    }

    private static RadioBlockEntity radio(GameTestHelper helper, BlockPos pos) {
        if(helper.getBlockEntity(pos) instanceof RadioBlockEntity radio)
            return radio;
        throw new IllegalStateException("No radio at " + pos);
    }
}
