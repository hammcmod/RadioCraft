package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.TapePlayerBlockEntity;
import com.arrl.radiocraft.common.blocks.TapePlayerBlock;
import com.arrl.radiocraft.common.datacomponents.TapeContents;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftDataComponent;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.items.TapeItem;
import com.arrl.radiocraft.common.radio.tape.TapeRecordings;
import com.arrl.radiocraft.common.radio.voice.RepeaterAudio;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** The tape player: plays a recording through, again and again, on a redstone pulse, and records on a blank tape. */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class TapeGameTests {

	/** A tape with half a second of a recorded tone on it. */
	private static ItemStack recordedTape(GameTestHelper helper) {
		UUID id = UUID.randomUUID();
		short[] audio = new short[24000];
		for(int i = 0; i < audio.length; i++)
			audio[i] = (short)(8000 * Math.sin(2 * Math.PI * 600 * i / 48000.0));
		TapeRecordings.save(helper.getLevel().getServer(), id, audio);
		ItemStack tape = new ItemStack(RadiocraftItems.TAPE_REEL.get());
		tape.set(RadiocraftDataComponent.TAPE_CONTENTS.get(), TapeContents.BLANK.withRecording(id, 1));
		return tape;
	}

	private static TapePlayerBlockEntity player(GameTestHelper helper, BlockPos pos) {
		return GameTestUtil.blockEntity(helper, pos, TapePlayerBlockEntity.class);
	}

	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100)
	public static void playsARecordingThrough(GameTestHelper helper) {
		BlockPos pos = new BlockPos(4, 2, 4);
		helper.setBlock(pos, RadiocraftBlocks.TAPE_PLAYER.get());
		helper.startSequence()
				.thenExecute(() -> {
					helper.assertTrue(player(helper, pos).insert(recordedTape(helper)), "the tape did not go in");
					player(helper, pos).play();
				})
				.thenWaitUntil(() -> helper.assertTrue(player(helper, pos).getState() == TapePlayerBlockEntity.STATE_PLAYING, "not playing"))
				.thenExecute(() -> helper.assertTrue(helper.getBlockState(pos).getValue(TapePlayerBlock.RUNNING), "the reels are still"))
				.thenWaitUntil(() -> helper.assertTrue(player(helper, pos).getState() == TapePlayerBlockEntity.STATE_IDLE, "it never reached the end"))
				.thenExecute(() -> helper.assertFalse(helper.getBlockState(pos).getValue(TapePlayerBlock.RUNNING), "the reels kept turning"))
				.thenSucceed();
	}

	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
	public static void loopsAndAnswersRedstone(GameTestHelper helper) {
		BlockPos loop = new BlockPos(3, 2, 3), pulse = new BlockPos(9, 2, 9);
		helper.setBlock(loop, RadiocraftBlocks.TAPE_PLAYER.get());
		helper.setBlock(pulse, RadiocraftBlocks.TAPE_PLAYER.get());
		int[] starts = { 0 };
		boolean[] wasPlaying = { false };
		helper.startSequence()
				.thenExecute(() -> {
					player(helper, loop).insert(recordedTape(helper));
					player(helper, loop).setMode(TapePlayerBlockEntity.Mode.LOOP);
					player(helper, pulse).insert(recordedTape(helper));
					player(helper, pulse).setMode(TapePlayerBlockEntity.Mode.REDSTONE);
				})
				.thenWaitUntil(() -> { // Counts the loop player starting over, three times.
					boolean playing = player(helper, loop).getState() == TapePlayerBlockEntity.STATE_PLAYING;
					if(playing && !wasPlaying[0])
						starts[0]++;
					wasPlaying[0] = playing;
					helper.assertTrue(starts[0] >= 3, "started " + starts[0] + " times");
				})
				.thenExecute(() -> {
					helper.assertTrue(player(helper, pulse).getState() == TapePlayerBlockEntity.STATE_IDLE, "the redstone player started by itself");
					helper.setBlock(pulse.east(), Blocks.REDSTONE_BLOCK);
				})
				.thenWaitUntil(() -> helper.assertTrue(player(helper, pulse).getState() != TapePlayerBlockEntity.STATE_IDLE, "a pulse did not start it"))
				.thenSucceed();
	}

	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
	public static void recordsOnABlankTape(GameTestHelper helper) {
		BlockPos pos = new BlockPos(6, 2, 6);
		helper.setBlock(pos, RadiocraftBlocks.TAPE_PLAYER.get());
		helper.runAfterDelay(2, () -> {
			TapePlayerBlockEntity player = player(helper, pos);
			player.insert(new ItemStack(RadiocraftItems.TAPE_REEL.get()));
			player.record();
			helper.assertTrue(player.canTransmitVoice(), "it should be listening");
			player.acceptVoicePacket(null, RepeaterAudio.tone(500.0D, 0.02D, 0.5D).get(0), UUID.randomUUID(), helper.absoluteVec(pos.getCenter()));
			player.stop();
			TapeContents contents = TapeItem.contents(player.getTape());
			helper.assertTrue(contents.isRecorded() && contents.recordingId() != null, "nothing was put on the tape");
			helper.assertTrue(TapeRecordings.audio(helper.getLevel().getServer(), contents.recordingId()) != null, "the recording is not kept");
			helper.succeed();
		});
	}
}
