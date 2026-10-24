package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Ionospheric Processor (maintainer's model, 2026-10-23): a rack two blocks tall with three glowing valves on top
 * that light the room a little ({@link #VALVE_LIGHT}), placed and broken as one. For now it is only the block; it will
 * read the sky through an Ionosonde Antenna on its coax (see doc/TncRework.md).
 */
public class IonosphericProcessorBlock extends Block {

	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	/** Light the valves give off, from the top half: a soft glow around the rack, about half a torch. */
	public static final int VALVE_LIGHT = 7;

	public IonosphericProcessorBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HALF, FACING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		if(pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context))
			return null;
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(HALF, DoubleBlockHalf.LOWER);
	}

	@Override
	public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
		level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
	}

	/** Either half goes when the other does, like a door. */
	@Override
	protected @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighbour, @NotNull LevelAccessor level,
	                                          @NotNull BlockPos pos, @NotNull BlockPos neighbourPos) {
		DoubleBlockHalf half = state.getValue(HALF);
		if(direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP))
			return neighbour.is(this) && neighbour.getValue(HALF) != half ? state : Blocks.AIR.defaultBlockState();
		return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
	}

	@Override
	protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
		if(state.getValue(HALF) == DoubleBlockHalf.LOWER)
			return true;
		BlockState below = level.getBlockState(pos.below());
		return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
	}

	/** In creative, breaking the top half must not let the bottom half drop the item. */
	@Override
	public @NotNull BlockState playerWillDestroy(Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
		if(!level.isClientSide && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
			BlockPos below = pos.below();
			BlockState belowState = level.getBlockState(below);
			if(belowState.is(this) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER) {
				level.setBlock(below, belowState.getFluidState().is(Fluids.WATER) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 35);
				level.levelEvent(player, 2001, below, Block.getId(belowState));
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
		tooltipComponents.add(Component.translatable("tooltip.radiocraft.not_implemented"));
	}
}
