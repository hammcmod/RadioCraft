package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A WEFAX receiver (model by the maintainer, 2026-10-23). Decorative for now: it will print weather charts once the
 * WX work in doc/TncRework.md is done.
 */
public class WefaxReceiverBlock extends Block {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		// The model faces north: a full 3 px base, and the printer body from z 2 back, 8 high.
		VoxelShape base = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D);
		SHAPES.put(Direction.NORTH, Shapes.or(base, Block.box(0.0D, 3.0D, 2.0D, 16.0D, 8.0D, 16.0D)));
		SHAPES.put(Direction.SOUTH, Shapes.or(base, Block.box(0.0D, 3.0D, 0.0D, 16.0D, 8.0D, 14.0D)));
		SHAPES.put(Direction.EAST, Shapes.or(base, Block.box(0.0D, 3.0D, 0.0D, 14.0D, 8.0D, 16.0D)));
		SHAPES.put(Direction.WEST, Shapes.or(base, Block.box(2.0D, 3.0D, 0.0D, 16.0D, 8.0D, 16.0D)));
	}

	public WefaxReceiverBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
		tooltipComponents.add(Component.translatable("tooltip.radiocraft.not_implemented"));
	}
}
