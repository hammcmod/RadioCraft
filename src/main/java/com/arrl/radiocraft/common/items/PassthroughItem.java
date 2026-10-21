package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.common.blockentities.PassthroughBlockEntity;
import com.arrl.radiocraft.common.blocks.PassthroughBlock;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Use on a plain solid block to turn it into a {@link PassthroughBlock}: it keeps its look and lets wire and coax run
 * through it. Only full, opaque blocks without a block entity qualify (stone, planks, bricks...), never wires or
 * devices.
 */
public class PassthroughItem extends Item {

	public PassthroughItem(Properties properties) {
		super(properties);
	}

	public static boolean canConvert(Level level, BlockPos pos, BlockState state) {
		return !state.isAir()
				&& state.isCollisionShapeFullBlock(level, pos)
				&& state.isSolidRender(level, pos)
				&& !state.hasBlockEntity()
				&& !(state.getBlock() instanceof WireBlock)
				&& !WireBlock.isValidConnection(state, true)
				&& !WireBlock.isValidConnection(state, false);
	}

	@Override
	public @NotNull InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		if(!canConvert(level, pos, state))
			return InteractionResult.PASS;
		if(level.isClientSide)
			return InteractionResult.SUCCESS;

		level.setBlock(pos, RadiocraftBlocks.PASSTHROUGH.get().getBlockState(level, pos), 3);
		if(level.getBlockEntity(pos) instanceof PassthroughBlockEntity be)
			be.setMimic(state);
		level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.2F);
		if(context.getPlayer() == null || !context.getPlayer().isCreative())
			context.getItemInHand().shrink(1);
		return InteractionResult.CONSUME;
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
		tooltipComponents.add(Component.translatable("tooltip.radiocraft.passthrough"));
	}
}
