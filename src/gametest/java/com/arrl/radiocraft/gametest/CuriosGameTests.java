package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.radio.voice.handheld.PlayerRadio;
import com.arrl.radiocraft.common.radio.voice.handheld.PlayerRadioManager;
import com.arrl.radiocraft.compat.curios.CuriosCompat;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

/** A VHF handheld worn on the belt (Curios, in the dev runtime): the player radio finds it and the worn-radio key keys it. */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CuriosGameTests {

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
    public static void handheldWornOnTheBeltTalks(GameTestHelper helper) {
        helper.assertTrue(CuriosCompat.isLoaded(), "Curios is not in the dev runtime");
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes("belt-wearer".getBytes()), "belt-wearer"));
        player.getInventory().clearContent();
        PlayerRadioManager.onPlayerJoined(new PlayerEvent.PlayerLoggedInEvent(player));
        try {
            ItemStack handheld = new ItemStack(RadiocraftItems.VHF_HANDHELD.get());
            IVHFHandheldCapability cap = handheld.getCapability(RadiocraftCapabilities.VHF_HANDHELDS);
            helper.assertTrue(cap != null, "The handheld has no radio capability");
            cap.setPowered(true);
            var curios = CuriosApi.getCuriosInventory(player).orElseThrow();
            helper.assertTrue(curios.getStacksHandler("belt").isPresent(), "Players have no belt slot");
            curios.setEquippedCurio("belt", 0, handheld);
            helper.assertTrue(CuriosCompat.wornHandhelds(player).size() == 1, "The worn handheld was not found");

            PlayerRadio radio = PlayerRadioManager.get(player.getUUID()).orElseThrow();
            radio.tick();
            helper.assertFalse(radio.canTransmitVoice(), "The worn radio transmits without its key held");
            radio.setWornPttHeld(true);
            radio.tick();
            helper.assertTrue(radio.canTransmitVoice(), "The worn-radio key did not key the worn handheld");
        } finally {
            PlayerRadioManager.onPlayerLeft(new PlayerEvent.PlayerLoggedOutEvent(player));
        }
        helper.succeed();
    }
}
