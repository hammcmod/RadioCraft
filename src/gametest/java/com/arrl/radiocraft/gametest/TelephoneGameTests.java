package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity;
import com.arrl.radiocraft.common.blockentities.TelephoneBlockEntity;
import com.arrl.radiocraft.common.blocks.TelephoneBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.items.TelephoneHandsetItem;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * The telephone's handset: sneak-right-click picks it up (answering a ringing phone) and hides it on the model, sneak-
 * right-click with it hangs up, and walking off with it snaps the cord back and ends the call. Placed against the side
 * of a block it goes on the wall.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class TelephoneGameTests {

	private static final String BATCH = "telephone";
	/** One microwave radio with a phone touching it on each side: they can call each other without a link. */
	private static final BlockPos RADIO = new BlockPos(5, 2, 5);
	private static final BlockPos PHONE_A = RADIO.north();
	private static final BlockPos PHONE_B = RADIO.south();

	private static TelephoneBlockEntity phone(GameTestHelper helper, BlockPos pos) {
		return GameTestUtil.blockEntity(helper, pos, TelephoneBlockEntity.class);
	}

	private static ServerPlayer player(GameTestHelper helper, String name) {
		FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
		player.getInventory().clearContent();
		if(!helper.getLevel().players().contains(player))
			helper.getLevel().players().add(player);
		return player;
	}

	private static void stand(GameTestHelper helper, ServerPlayer player, BlockPos near, double blocksEast) {
		Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(near)).add(blocksEast, 0.0D, 0.0D);
		player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
	}

	private static void exchange(GameTestHelper helper) {
		helper.setBlock(RADIO, RadiocraftBlocks.MICROWAVE_RADIO.get());
		helper.setBlock(PHONE_A, RadiocraftBlocks.TELEPHONE.get());
		helper.setBlock(PHONE_B, RadiocraftBlocks.TELEPHONE.get());
	}

	private static boolean offHook(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockState(pos).getValue(TelephoneBlock.OFF_HOOK);
	}

	/** A rings B; picking up B's handset answers, the model loses its handset; hanging it up ends the call at both ends. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
	public static void handsetAnswersAndHangsUp(GameTestHelper helper) {
		exchange(helper);
		ServerPlayer player = player(helper, "phone-answerer");
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> GameTestUtil.blockEntity(helper, RADIO, MicrowaveRadioBlockEntity.class).togglePower())
				.thenWaitUntil(() -> helper.assertTrue(GameTestUtil.blockEntity(helper, RADIO, MicrowaveRadioBlockEntity.class).isPowered(), "the microwave radio is not up"))
				.thenExecute(() -> {
					phone(helper, PHONE_A).dial(phone(helper, PHONE_B).getExtension());
					helper.assertTrue(phone(helper, PHONE_B).getState() == TelephoneBlockEntity.State.RINGING, "B is not ringing: " + phone(helper, PHONE_A).getStatus());
					stand(helper, player, PHONE_B, 1.5D);
					player.setShiftKeyDown(true);
					helper.useBlock(PHONE_B, player);
					helper.assertTrue(TelephoneHandsetItem.isHandsetOf(player.getMainHandItem(), phone(helper, PHONE_B)), "no handset in the hand: " + player.getMainHandItem());
					helper.assertTrue(phone(helper, PHONE_B).getState() == TelephoneBlockEntity.State.IN_CALL, "picking up did not answer");
					helper.assertTrue(offHook(helper, PHONE_B), "the handset still shows on B");
					helper.assertFalse(offHook(helper, PHONE_A), "A's handset went too");
				})
				.thenIdle(5)
				.thenExecute(() -> {
					helper.assertTrue(phone(helper, PHONE_B).getState() == TelephoneBlockEntity.State.IN_CALL, "the call dropped with the handset up");
					player.setShiftKeyDown(false);
					helper.useBlock(PHONE_B, player); // A plain right click with it opens the keypad, and does not hang up.
					helper.assertTrue(phone(helper, PHONE_B).getState() == TelephoneBlockEntity.State.IN_CALL, "a plain right click hung up");
					player.setShiftKeyDown(true);
					helper.useBlock(PHONE_B, player);
					helper.assertTrue(player.getMainHandItem().isEmpty(), "the handset stayed in the hand");
					helper.assertFalse(offHook(helper, PHONE_B), "B still shows off the hook");
					helper.assertTrue(phone(helper, PHONE_A).getState() == TelephoneBlockEntity.State.IDLE
							&& phone(helper, PHONE_B).getState() == TelephoneBlockEntity.State.IDLE, "hanging up did not end the call");
					helper.getLevel().players().remove(player);
				})
				.thenSucceed();
	}

	/** Walking more than the cord's length away with the handset: it snaps back and the call ends. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
	public static void cordSnapsBack(GameTestHelper helper) {
		exchange(helper);
		ServerPlayer player = player(helper, "phone-walker");
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> GameTestUtil.blockEntity(helper, RADIO, MicrowaveRadioBlockEntity.class).togglePower())
				.thenWaitUntil(() -> helper.assertTrue(GameTestUtil.blockEntity(helper, RADIO, MicrowaveRadioBlockEntity.class).isPowered(), "the microwave radio is not up"))
				.thenExecute(() -> {
					stand(helper, player, PHONE_A, 1.5D);
					player.setShiftKeyDown(true);
					helper.useBlock(PHONE_A, player);
					player.setShiftKeyDown(false);
					helper.assertTrue(offHook(helper, PHONE_A), "the handset did not come up");
					phone(helper, PHONE_A).dial(phone(helper, PHONE_B).getExtension());
					helper.assertTrue(phone(helper, PHONE_A).getState() == TelephoneBlockEntity.State.CALLING, "A is not calling");
					stand(helper, player, PHONE_A, TelephoneBlockEntity.CORD_LENGTH + 2.0D);
				})
				.thenWaitUntil(() -> helper.assertFalse(offHook(helper, PHONE_A), "the cord did not snap back"))
				.thenExecute(() -> {
					helper.assertTrue(player.getMainHandItem().isEmpty(), "the handset stayed with the player");
					helper.assertTrue(phone(helper, PHONE_A).getHandsetHolder() == null, "A still thinks its handset is out");
					helper.assertTrue(phone(helper, PHONE_B).getState() == TelephoneBlockEntity.State.IDLE, "B is still ringing");
					helper.getLevel().players().remove(player);
				})
				.thenSucceed();
	}

	/** Placed on the side of a block it goes on the wall facing out; on top of one, on the desk facing the player. */
	@GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20, batch = BATCH)
	public static void goesOnTheWall(GameTestHelper helper) {
		ServerPlayer player = player(helper, "phone-hanger");
		BlockPos wall = new BlockPos(4, 2, 4);
		helper.setBlock(wall, net.minecraft.world.level.block.Blocks.STONE);
		ItemStack phone = new ItemStack(RadiocraftItems.TELEPHONE.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, phone);
		BlockPos absolute = helper.absolutePos(wall);
		BlockState onWall = RadiocraftBlocks.TELEPHONE.get().getStateForPlacement(new BlockPlaceContext(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, phone,
				new BlockHitResult(Vec3.atCenterOf(absolute).add(0.5D, 0.0D, 0.0D), Direction.EAST, absolute, false))));
		helper.assertTrue(onWall != null && onWall.getValue(TelephoneBlock.WALL) && onWall.getValue(TelephoneBlock.FACING) == Direction.EAST,
				"clicking the east side gave " + onWall);
		BlockState onDesk = RadiocraftBlocks.TELEPHONE.get().getStateForPlacement(new BlockPlaceContext(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, phone,
				new BlockHitResult(Vec3.atCenterOf(absolute).add(0.0D, 0.5D, 0.0D), Direction.UP, absolute, false))));
		helper.assertTrue(onDesk != null && !onDesk.getValue(TelephoneBlock.WALL), "clicking the top gave " + onDesk);
		helper.getLevel().players().remove(player);
		helper.succeed();
	}
}
