package com.arrl.radiocraft.common.blocks.antennas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

public class AntennaConnectorBlock extends Block {

    public static final DirectionProperty PLACED_ON = DirectionProperty.create("placed_on");
    public static final HashMap<Direction, VoxelShape> SHAPES = new HashMap<>();

    static {
        SHAPES.put(Direction.NORTH, Block.box(6.0D, 6.0D, 8.0D, 10.0D, 10.0D, 16.0D));
        SHAPES.put(Direction.SOUTH, Block.box(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 8.0D));
        SHAPES.put(Direction.EAST, Block.box(0.0D, 6.0D, 6.0D, 8.0D, 10.0D, 10.0D));
        SHAPES.put(Direction.WEST, Block.box(8.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D));
        SHAPES.put(Direction.UP, Block.box(6.0D, 0.0D, 6.0D, 10.0D, 8.0D, 10.0D));
        SHAPES.put(Direction.DOWN, Block.box(6.0D, 8.0D, 6.0D, 10.0D, 16.0D, 10.0D));
    }

    public AntennaConnectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(PLACED_ON);
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPES.get(state.getValue(PLACED_ON));
    }

    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPES.get(state.getValue(PLACED_ON));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(PLACED_ON, context.getClickedFace());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.radiocraft.not_implemented"));
    }
}
