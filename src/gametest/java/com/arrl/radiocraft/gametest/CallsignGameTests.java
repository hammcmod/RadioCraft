package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.data.PlayerCallsignSavedData;
import com.arrl.radiocraft.common.radio.Callsigns;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Players choosing callsigns, and repeater callsigns (proposal; maintainer, 2026-10-24). */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CallsignGameTests {

    private static ServerPlayer player(GameTestHelper helper, String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
    }

    private static void forget(GameTestHelper helper, ServerPlayer... players) {
        for(ServerPlayer player : players)
            PlayerCallsignSavedData.get(helper.getLevel().getServer().overworld()).resetCallsign(player.getUUID());
    }

    /** The admin /callsign generate makes valid callsigns from the prefix list, the same one each time for a player. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20, batch = "callsigns")
    public static void adminGeneratedCallsignsAreValid(GameTestHelper helper) {
        for(int i = 0; i < 50; i++) {
            UUID id = UUID.nameUUIDFromBytes(("generated-" + i).getBytes());
            String callsign = com.arrl.radiocraft.common.commands.CallsignCommands.generateCallsignFromUuid(id);
            helper.assertTrue(Callsigns.isValidPlayerCallsign(callsign), "Generated " + callsign + " is not a valid callsign");
            helper.assertTrue(callsign.equals(com.arrl.radiocraft.common.commands.CallsignCommands.generateCallsignFromUuid(id)), "Generation is not repeatable");
        }
        helper.succeed();
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20, batch = "callsigns")
    public static void playerCallsignForm(GameTestHelper helper) {
        for(String good : new String[] { "K1ABC", "W4X", "KA1B", "N0ZZ", "VE3XY", "2E0AB", "G4ABC", "KH6XX" })
            helper.assertTrue(Callsigns.isValidPlayerCallsign(good), good + " was refused");
        for(String bad : new String[] { "1ABC", "K1", "K12AB", "K1ABCD", "KAB1C", "QQ1ABC", "XZ1AB", "" })
            helper.assertFalse(Callsigns.isValidPlayerCallsign(bad), bad + " was accepted");
        String digits = RadiocraftServerConfig.CALLSIGN_DIGITS.get();
        RadiocraftServerConfig.CALLSIGN_DIGITS.set("3");
        boolean one = Callsigns.isValidPlayerCallsign("K1ABC"), three = Callsigns.isValidPlayerCallsign("K3ABC");
        RadiocraftServerConfig.CALLSIGN_DIGITS.set(digits);
        helper.assertFalse(one, "A digit outside the pool was accepted");
        helper.assertTrue(three, "A digit in the pool was refused");
        helper.succeed();
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20, batch = "callsigns")
    public static void claimingCallsigns(GameTestHelper helper) {
        ServerPlayer a = player(helper, "callsign-a"), b = player(helper, "callsign-b");
        forget(helper, a, b);
        Callsigns.ClaimResult first = Callsigns.claim(a, "k7qrz");
        Callsigns.ClaimResult again = Callsigns.claim(a, "K7QRY");
        Callsigns.ClaimResult taken = Callsigns.claim(b, "K7QRZ");
        Callsigns.ClaimResult invalid = Callsigns.claim(b, "77QRZ");
        String mine = Callsigns.playerCallsign(helper.getLevel().getServer(), a.getUUID());
        Callsigns.ClaimResult chosen = Callsigns.claim(b, Callsigns.CHOOSE_TOKEN);
        String theirs = Callsigns.playerCallsign(helper.getLevel().getServer(), b.getUUID());
        forget(helper, a, b);
        helper.assertTrue(chosen == Callsigns.ClaimResult.OK && theirs != null && Callsigns.isValidPlayerCallsign(theirs)
                && theirs.indexOf((char)('0' + Callsigns.zoneDigit(helper.getLevel(), b.blockPosition()))) >= 0, "Choose one for me gave " + chosen + " " + theirs);
        helper.assertTrue(first == Callsigns.ClaimResult.OK && "K7QRZ".equals(mine), "The first claim failed: " + first + " " + mine);
        helper.assertTrue(again == Callsigns.ClaimResult.ALREADY_HAS, "A second claim was " + again);
        helper.assertTrue(taken == Callsigns.ClaimResult.TAKEN, "Claiming a taken callsign was " + taken);
        helper.assertTrue(invalid == Callsigns.ClaimResult.INVALID, "An invalid callsign was " + invalid);
        helper.succeed();
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20, batch = "callsigns")
    public static void repeaterCallsigns(GameTestHelper helper) {
        ServerPlayer owner = player(helper, "callsign-owner");
        forget(helper, owner);
        Callsigns.claim(owner, "W9OWN");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        int digit = Callsigns.zoneDigit(helper.getLevel(), pos);
        String auto = Callsigns.generateRepeater(helper.getLevel(), pos, helper.getLevel().getRandom());
        Callsigns.RepeaterMode mode = RadiocraftServerConfig.REPEATER_CALLSIGN_MODE.get();
        try {
            helper.assertTrue(auto.length() == 4 && auto.charAt(1) == (char)('0' + digit) && Callsigns.isValidRepeaterCallsign(helper.getLevel(), pos, auto),
                    "Made-up callsign " + auto + " is not X" + digit + "XX");
            helper.assertTrue("KNWGMFI".indexOf(auto.charAt(0)) >= 0, "Made-up callsign " + auto + " does not start with a one-letter prefix");
            String q = "Q" + digit + "AB";
            helper.assertFalse(Callsigns.isValidRepeaterCallsign(helper.getLevel(), pos, q), q + " was accepted: Q is no prefix");
            RadiocraftServerConfig.REPEATER_CALLSIGN_MODE.set(Callsigns.RepeaterMode.EITHER);
            helper.assertTrue("W9OWN".equals(Callsigns.resolveRepeaterCallsign(owner, helper.getLevel(), pos, Callsigns.OWN_TOKEN)), "EITHER refused the owner's callsign");
            helper.assertTrue(auto.equals(Callsigns.resolveRepeaterCallsign(owner, helper.getLevel(), pos, auto)), "EITHER refused an X#XX callsign");
            helper.assertTrue(Callsigns.resolveRepeaterCallsign(owner, helper.getLevel(), pos, "QQQQQ") == null, "EITHER took anything");
            RadiocraftServerConfig.REPEATER_CALLSIGN_MODE.set(Callsigns.RepeaterMode.AUTO);
            helper.assertTrue(Callsigns.resolveRepeaterCallsign(owner, helper.getLevel(), pos, "W9OWN") == null, "AUTO took the owner's callsign");
            RadiocraftServerConfig.REPEATER_CALLSIGN_MODE.set(Callsigns.RepeaterMode.OWN);
            helper.assertTrue(Callsigns.resolveRepeaterCallsign(owner, helper.getLevel(), pos, Callsigns.AUTO_TOKEN) == null, "OWN made one up");
        }
        finally {
            RadiocraftServerConfig.REPEATER_CALLSIGN_MODE.set(mode);
            forget(helper, owner);
        }
        helper.succeed();
    }
}
