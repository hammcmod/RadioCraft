package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.voice.RadioModeration;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** {@code /radio moderateall} and {@code /radio moderate <player>}: the commands exist and the listening state follows them. */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ModerationGameTests {

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20)
    public static void moderationCommandsAndState(GameTestHelper helper) {
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        for(String root : new String[] { "radio", "radiocraft" }) {
            var node = dispatcher.getRoot().getChild(root);
            helper.assertTrue(node != null && node.getChild("moderateall") != null && node.getChild("moderate") != null, "/" + root + " has no moderate commands");
        }
        ServerPlayer moderator = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes("moderator".getBytes()), "moderator"));
        UUID target = UUID.nameUUIDFromBytes("talker".getBytes());
        RadioModeration.listen(moderator, target);
        boolean one = target.equals(RadioModeration.listeningTo(moderator.getUUID()));
        RadioModeration.listen(moderator, RadioModeration.EVERYONE);
        boolean all = RadioModeration.EVERYONE.equals(RadioModeration.listeningTo(moderator.getUUID()));
        boolean stopped = RadioModeration.stop(moderator.getUUID()) && RadioModeration.listeningTo(moderator.getUUID()) == null;
        helper.assertTrue(one && all && stopped, "Moderation state: one " + one + ", all " + all + ", stopped " + stopped);
        helper.succeed();
    }
}
