package com.arrl.radiocraft.common.blocks.antennas;

import com.arrl.radiocraft.common.blockentities.AntennaBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Antenna Pole Block (place from the bottom max of 10) based on ScaffoldingBlock
 */
public class AntennaPoleBlock extends Block implements SimpleWaterloggedBlock {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D));
    public static final int STABILITY_MAX_DISTANCE = 10;
    public static final IntegerProperty DISTANCE = IntegerProperty.create("distance", 0, STABILITY_MAX_DISTANCE);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty BOTTOM = BlockStateProperties.BOTTOM;

    @SuppressWarnings("this-escape")
    public AntennaPoleBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DISTANCE, STABILITY_MAX_DISTANCE).setValue(WATERLOGGED, Boolean.FALSE).setValue(BOTTOM, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DISTANCE, WATERLOGGED, BOTTOM);
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean canBeReplaced(@NotNull BlockState state, BlockPlaceContext context) {
        return context.getItemInHand().is(this.asItem());
    }

    @Override
    public @NotNull BlockState updateShape(BlockState state, @NotNull Direction facing, @NotNull BlockState facingState, @NotNull LevelAccessor level, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos) {
        if (state.getValue(WATERLOGGED))
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));

        if (!level.isClientSide())
            level.scheduleTick(currentPos, this, 1);

        return state;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return getDistance(level, pos) < 7;
    }

    @Override
    public void tick(BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        int i = getDistance(level, pos);
        BlockState blockstate = state.setValue(DISTANCE, i).setValue(BOTTOM, i == 0);
        if (blockstate.getValue(DISTANCE) == STABILITY_MAX_DISTANCE) {

            if (state.getValue(DISTANCE) == STABILITY_MAX_DISTANCE)
                FallingBlockEntity.fall(level, pos, blockstate);
            else
                level.destroyBlock(pos, true);

        }
        else if (state != blockstate)
            level.setBlock(pos, blockstate, 3);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos blockpos = context.getClickedPos();
        Level level = context.getLevel();
        int i = getDistance(level, blockpos);
        return this.defaultBlockState().setValue(WATERLOGGED, level.getFluidState(blockpos).getType() == Fluids.WATER).setValue(DISTANCE, i).setValue(BOTTOM, i == 0);
    }

    public static int getDistance(BlockGetter level, BlockPos pos) {
        MutableBlockPos mutablePos = pos.mutable().move(Direction.DOWN);
        BlockState state = level.getBlockState(mutablePos);
        int i = STABILITY_MAX_DISTANCE;

        if (state.is(RadiocraftBlocks.ANTENNA_POLE.get()))
            i = Math.min(i, state.getValue(DISTANCE) + 1);
        else if(state.isFaceSturdy(level, mutablePos, Direction.UP) || state.getBlock() instanceof BalunBlock)
            return 0;

        return i;
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
        MutableBlockPos bottomPos = pos.mutable().move(Direction.DOWN);
        for(int i = 0; i < STABILITY_MAX_DISTANCE; i++) {
            if(level.getBlockState(bottomPos).getBlock() != RadiocraftBlocks.ANTENNA_POLE.get()) {
                if(level.getBlockEntity(bottomPos) instanceof AntennaBlockEntity be)
                    be.markAntennaChanged();
                return;
            }
            bottomPos.move(Direction.DOWN);
        }
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        MutableBlockPos bottomPos = pos.mutable().move(Direction.DOWN);
        for(int i = 0; i < STABILITY_MAX_DISTANCE; i++) {
            if(level.getBlockState(bottomPos).getBlock() != RadiocraftBlocks.ANTENNA_POLE.get()) {
                if(level.getBlockEntity(bottomPos) instanceof AntennaBlockEntity be)
                    be.markAntennaChanged();
                return;
            }
            bottomPos.move(Direction.DOWN);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.radiocraft.not_implemented"));
    }
}
