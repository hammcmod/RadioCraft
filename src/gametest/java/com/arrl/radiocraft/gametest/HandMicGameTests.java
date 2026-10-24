package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.items.CordedMicrophoneItem;
import com.arrl.radiocraft.common.radio.voice.handheld.PlayerRadioManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * The hand mic on its cord: sneak-right-click a radio with an empty hand to take it, hold right click to talk, right
 * click the radio to hang it up, walk more than 5 blocks away and the cord snaps it back. The player is a NeoForge
 * fake player put into the level's player list (so the radio can find it) with a player radio (for the use-key
 * state), and taken out again at the end. A real mock login would have mods send it network packets.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class HandMicGameTests {

    private static final BlockPos RADIO = new BlockPos(3, 2, 3);

    private static HFRadioBlockEntity radio(GameTestHelper helper) {
        return GameTestUtil.blockEntity(helper, RADIO, HFRadioBlockEntity.class);
    }

    private static void stand(GameTestHelper helper, ServerPlayer player, double blocksEast) {
        Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(RADIO)).add(blocksEast, 0.0D, 0.0D);
        player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
    }

    /** A fake player the radio can find by UUID, with a player radio for the use key. */
    private static ServerPlayer player(GameTestHelper helper, String name) {
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
        player.getInventory().clearContent();
        if(!helper.getLevel().players().contains(player))
            helper.getLevel().players().add(player);
        PlayerRadioManager.onPlayerJoined(new PlayerEvent.PlayerLoggedInEvent(player));
        return player;
    }

    private static void remove(GameTestHelper helper, ServerPlayer player) {
        PlayerRadioManager.onPlayerLeft(new PlayerEvent.PlayerLoggedOutEvent(player));
        helper.getLevel().players().remove(player);
    }

    private static boolean holdsMic(GameTestHelper helper, ServerPlayer player) {
        return CordedMicrophoneItem.isMicOf(player.getMainHandItem(), radio(helper));
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void handMicTalksHangsUpAndSnapsBack(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        ServerPlayer player = player(helper, "mic-tester");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    TransmissionGameTests.setupSsb(helper, RADIO, 400);
                    stand(helper, player, 2.0D);
                    player.setShiftKeyDown(true);
                    helper.useBlock(RADIO, player);
                    helper.assertTrue(holdsMic(helper, player), "Sneak-use did not put the mic in the player's hand: " + player.getMainHandItem());
                    helper.assertTrue(player.getUUID().equals(radio(helper).getMicHolder()), "The radio does not know who has its mic");
                    player.setShiftKeyDown(false);
                    PlayerRadioManager.get(player.getUUID()).orElseThrow().setUseHeld(true);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(radio(helper).isPTTDown(), "Holding right click with the mic did not key the radio");
                    helper.assertTrue(radio(helper).canTransmitVoice(), "The keyed radio cannot transmit");
                    PlayerRadioManager.get(player.getUUID()).orElseThrow().setUseHeld(false);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertFalse(radio(helper).isPTTDown(), "Letting go did not unkey the radio");
                    helper.useBlock(RADIO, player); // Right click the radio with the mic: hang it up.
                    helper.assertTrue(player.getMainHandItem().isEmpty(), "The mic stayed in the hand after hanging up");
                    helper.assertTrue(radio(helper).getMicHolder() == null, "The radio still thinks its mic is out");
                    player.setShiftKeyDown(true);
                    helper.useBlock(RADIO, player); // Take it again...
                    player.setShiftKeyDown(false);
                    helper.assertTrue(holdsMic(helper, player), "Could not take the mic a second time");
                    stand(helper, player, 6.0D); // ...and walk off with it.
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(radio(helper).getMicHolder() == null, "The cord did not snap at 6 blocks");
                    helper.assertFalse(player.getInventory().contains(stack -> stack.getItem() instanceof CordedMicrophoneItem), "The mic stayed with the player");
                    remove(helper, player);
                })
                .thenSucceed();
    }

    /** A receiver has no hand mic: sneak-use does nothing. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
    public static void receiverHasNoHandMic(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RECEIVER.get());
        ServerPlayer player = player(helper, "receiver-tester");
        helper.runAfterDelay(2, () -> {
            stand(helper, player, 2.0D);
            player.setShiftKeyDown(true);
            helper.useBlock(RADIO, player);
            helper.assertTrue(player.getMainHandItem().isEmpty(), "A receiver lent out a mic");
            remove(helper, player);
            helper.succeed();
        });
    }
}
