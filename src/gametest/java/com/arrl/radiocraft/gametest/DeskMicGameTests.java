package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.blocks.radios.MicrophoneBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.voice.handheld.PlayerRadioManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * The desk microphone: hold right click on it to talk, sneak and right click to lock talking on, and it lets go by
 * itself after the timeout. A fake player in the level's player list, as in {@link HandMicGameTests}.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DeskMicGameTests {

    private static final BlockPos RADIO = new BlockPos(4, 2, 4);
    /** Facing north, the mic's left is east: a radio to its west is on its right. */
    private static final BlockPos MIC = RADIO.east();

    private static RadioBlockEntity radio(GameTestHelper helper) {
        return GameTestUtil.blockEntity(helper, RADIO, HFRadioBlockEntity.class);
    }

    private static ServerPlayer player(GameTestHelper helper) {
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes("desk-mic".getBytes()), "desk-mic"));
        player.getInventory().clearContent();
        if(!helper.getLevel().players().contains(player))
            helper.getLevel().players().add(player);
        PlayerRadioManager.onPlayerJoined(new PlayerEvent.PlayerLoggedInEvent(player));
        Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(MIC)).add(0.0D, 0.0D, -2.0D);
        player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        return player;
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = "desk_mic")
    public static void holdLockAndTimeout(GameTestHelper helper) {
        int timeout = RadiocraftServerConfig.DESK_MIC_TIMEOUT_SECONDS.get();
        RadiocraftServerConfig.DESK_MIC_TIMEOUT_SECONDS.set(2);
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        helper.setBlock(MIC, net.minecraft.world.level.block.Block.updateFromNeighbourShapes(
                RadiocraftBlocks.MICROPHONE.get().defaultBlockState().setValue(MicrophoneBlock.FACING, Direction.NORTH), helper.getLevel(), helper.absolutePos(MIC)));
        ServerPlayer player = player(helper);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(MicrophoneBlock.radio(helper.getBlockState(MIC), helper.getLevel(), helper.absolutePos(MIC)) == radio(helper), "The mic did not find its radio");
                    PlayerRadioManager.get(player.getUUID()).orElseThrow().setUseHeld(true);
                    helper.useBlock(MIC, player);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(radio(helper).isPTTDown(), "Holding right click on the desk mic did not key the radio");
                    PlayerRadioManager.get(player.getUUID()).orElseThrow().setUseHeld(false);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertFalse(radio(helper).isPTTDown(), "Letting go did not unkey the radio");
                    player.setShiftKeyDown(true);
                    helper.useBlock(MIC, player); // Lock.
                    player.setShiftKeyDown(false);
                })
                .thenIdle(5)
                .thenExecute(() -> helper.assertTrue(radio(helper).isPTTDown() && radio(helper).isDeskMicLocked(), "Locked, the radio is not keyed"))
                .thenIdle(45) // Past the 2 second timeout.
                .thenExecute(() -> {
                    helper.assertFalse(radio(helper).isPTTDown(), "The lock did not time out");
                    helper.assertFalse(radio(helper).isDeskMicLocked(), "Still locked after the timeout");
                    RadiocraftServerConfig.DESK_MIC_TIMEOUT_SECONDS.set(timeout);
                    PlayerRadioManager.onPlayerLeft(new PlayerEvent.PlayerLoggedOutEvent(player));
                    helper.getLevel().players().remove(player);
                })
                .thenSucceed();
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20)
    public static void deskMicGetsQuieterWithDistance(GameTestHelper helper) {
        helper.assertTrue(RadioBlockEntity.deskMicGain(1.0D) == 1.0D, "Up close is not full level");
        helper.assertTrue(Math.abs(RadioBlockEntity.deskMicGain(3.0D) - 0.5D) < 1e-9, "Three blocks off is not half");
        helper.assertTrue(RadioBlockEntity.deskMicGain(20.0D) == 0.0D, "The mic hears across the room");
        helper.succeed();
    }
}
