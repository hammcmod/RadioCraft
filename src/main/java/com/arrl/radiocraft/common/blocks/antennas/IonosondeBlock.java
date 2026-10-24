package com.arrl.radiocraft.common.blocks.antennas;

import com.arrl.radiocraft.common.blockentities.IonosondeBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The ionosonde antenna (maintainer's model, 2026-10-23): two blocks tall, placed and broken as one like the J-pole,
 * with the coax on the lower half. Coax beside the lower half shows a run in to the foot of the mast (one property per
 * side, absolute, not turned with FACING). See {@link com.arrl.radiocraft.common.radio.antenna.types.IonosondeAntennaType}.
 */
public final class IonosondeBlock extends DoubleVHFAntennaBlock {

    public static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    public IonosondeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }

    private static BooleanProperty side(Direction direction) {
        return switch(direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            default -> WEST;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if(state == null)
            return null;
        for(Direction direction : Direction.Plane.HORIZONTAL)
            state = state.setValue(side(direction), context.getLevel().getBlockState(context.getClickedPos().relative(direction)).is(RadiocraftBlocks.COAX_WIRE.get()));
        return state;
    }

    /** Coax laid or taken up beside the lower half shows or hides the run in to the mast. */
    @Override
    public @NotNull BlockState updateShape(BlockState state, Direction facing, @NotNull BlockState facingState, @NotNull LevelAccessor level, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos) {
        BlockState updated = super.updateShape(state, facing, facingState, level, currentPos, facingPos);
        if(updated.is(this) && facing.getAxis().isHorizontal() && updated.getValue(HALF) == DoubleBlockHalf.LOWER)
            return updated.setValue(side(facing), facingState.is(RadiocraftBlocks.COAX_WIRE.get()));
        return updated;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new IonosondeBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : IonosondeBlockEntity::tick;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.radiocraft.ionosonde"));
    }
}
