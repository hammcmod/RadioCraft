package com.arrl.radiocraft.common.blocks.radios;

import com.arrl.radiocraft.common.blockentities.radio.HFReceiverBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

public class HFReceiverBlock extends RadioBlock {

	public static final HashMap<Direction, VoxelShape> SHAPES = new HashMap<>();

	static {
		SHAPES.put(Direction.NORTH, Block.box(2.0D, 0.0D, 4.0D, 14.0D, 7.0D, 16.0D));
		SHAPES.put(Direction.SOUTH, Block.box(2.0D, 0.0D, 0.0D, 14.0D, 7.0D, 12.0D));
		SHAPES.put(Direction.WEST, Block.box(4.0D, 0.0D, 2.0D, 16.0D, 7.0D, 14.0D));
		SHAPES.put(Direction.EAST, Block.box(0.0D, 0.0D, 2.0D, 12.0D, 7.0D, 14.0D));
	}

	public HFReceiverBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return null;
	}

	@Override
	public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPES.get(state.getValue(RadioBlock.HORIZONTAL_FACING));
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new HFReceiverBlockEntity(pos, state);
	}

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.radiocraft.not_implemented"));
    }
}
