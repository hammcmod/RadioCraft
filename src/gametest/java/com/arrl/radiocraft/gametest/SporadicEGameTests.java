package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.commands.BandCalculateCommands;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import com.arrl.radiocraft.common.radio.solar.SporadicE;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Sporadic E (proposal; maintainer, 2026-10-24) and the solar events switch. They change the whole dimension's
 * weather, so each runs in a batch of its own and puts the weather back.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SporadicEGameTests {

    private static void event(GameTestHelper helper, String id) {
        SolarEventManager.setEvent(helper.getLevel(), RadiocraftData.SOLAR_EVENTS.getValue(Radiocraft.id(id)).getInstance());
    }

    /** Only good or great conditions allow an opening; while open, 2m carries far and /bandcalculate says so. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20, batch = "sporadic_e")
    public static void sporadicEOpensVhfInGoodConditions(GameTestHelper helper) {
        SolarEventInstance previous = SolarEventManager.getEvent(helper.getLevel());
        GameTestDefaults.calmSky(helper.getLevel()); // An opening or aurora left over from another batch carries 2m too.
        helper.assertFalse(SporadicE.conditionsAllow(helper.getLevel()), "Average conditions allow sporadic E");
        event(helper, "good");
        helper.assertTrue(SporadicE.conditionsAllow(helper.getLevel()), "Good conditions do not allow sporadic E");
        Band twoMetre = Band.getBand("2m");
        helper.assertTrue(SporadicE.vhfStrength(helper.getLevel(), twoMetre, 1500.0D) == 0.0D, "2m carries 1500 blocks while closed");
        SporadicE.open(helper.getLevel(), 2000);
        double open = SporadicE.vhfStrength(helper.getLevel(), twoMetre, 1500.0D);
        boolean listed = BandCalculateCommands.calculate(helper.getLevel(), BlockPos.ZERO, new BlockPos(1500, 0, 0)).stream()
                .anyMatch(r -> r.band().name().equals("2m") && r.path() == BandCalculateCommands.Path.SPORADIC_E);
        SporadicE.close(helper.getLevel());
        if(previous != null)
            SolarEventManager.setEvent(helper.getLevel(), previous);
        helper.assertTrue(open > 0.0D, "2m does not carry 1500 blocks on sporadic E");
        helper.assertTrue(listed, "/bandcalculate does not show sporadic E for 2m");
        helper.assertFalse(SporadicE.vhfStrength(helper.getLevel(), Band.getBand("20m"), 1500.0D) > 0.0D, "Sporadic E carries HF");
        helper.succeed();
    }

    /** With solar events off, the dimension settles on average conditions. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40, batch = "solar_switch")
    public static void solarEventsOffMeansAverage(GameTestHelper helper) {
        SolarEventInstance previous = SolarEventManager.getEvent(helper.getLevel());
        event(helper, "blackout");
        RadiocraftServerConfig.SOLAR_EVENTS_ENABLED.set(false);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    var id = RadiocraftData.SOLAR_EVENTS.getKey(SolarEventManager.getEvent(helper.getLevel()).getEvent());
                    RadiocraftServerConfig.SOLAR_EVENTS_ENABLED.set(true);
                    if(previous != null)
                        SolarEventManager.setEvent(helper.getLevel(), previous);
                    helper.assertTrue(id != null && id.getPath().equals("average"), "With solar events off the event is " + id);
                })
                .thenSucceed();
    }
}
