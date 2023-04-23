package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
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

/**
 * @author MoreThanHidden
 * Antenna Pole Block (place from the bottom max of 10) based on ScaffoldingBlock
 */
@SuppressWarnings("deprecation")
public class AntennaPoleBlock extends Block implements SimpleWaterloggedBlock {
    private static final VoxelShape SHAPE;
    public static final int STABILITY_MAX_DISTANCE = 10;
    public static final IntegerProperty DISTANCE = IntegerProperty.create("distance", 0, STABILITY_MAX_DISTANCE);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty BOTTOM = BlockStateProperties.BOTTOM;

    public AntennaPoleBlock(BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DISTANCE, STABILITY_MAX_DISTANCE).setValue(WATERLOGGED, Boolean.FALSE).setValue(BOTTOM, Boolean.FALSE));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(DISTANCE, WATERLOGGED, BOTTOM);
    }

    public @NotNull FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    public boolean canBeReplaced(@NotNull BlockState pState, BlockPlaceContext pUseContext) {
        return pUseContext.getItemInHand().is(this.asItem());
    }

    public @NotNull BlockState updateShape(BlockState pState, @NotNull Direction pFacing, @NotNull BlockState pFacingState, @NotNull LevelAccessor pLevel, @NotNull BlockPos pCurrentPos, @NotNull BlockPos pFacingPos) {
        if (pState.getValue(WATERLOGGED)) {
            pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
        }

        if (!pLevel.isClientSide()) {
            pLevel.scheduleTick(pCurrentPos, this, 1);
        }

        return pState;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState pState, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        return SHAPE;
    }

    static {
        SHAPE = Shapes.or(Block.box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D));
    }

    public boolean canSurvive(@NotNull BlockState pState, @NotNull LevelReader pLevel, @NotNull BlockPos pPos) {
        return getDistance(pLevel, pPos) < 7;
    }

    public void tick(BlockState pState, @NotNull ServerLevel pLevel, @NotNull BlockPos pPos, @NotNull RandomSource pRandom) {
        int i = getDistance(pLevel, pPos);
        BlockState blockstate = pState.setValue(DISTANCE, i).setValue(BOTTOM, i == 0);
        if (blockstate.getValue(DISTANCE) == STABILITY_MAX_DISTANCE) {
            if (pState.getValue(DISTANCE) == STABILITY_MAX_DISTANCE) {
                FallingBlockEntity.fall(pLevel, pPos, blockstate);
            } else {
                pLevel.destroyBlock(pPos, true);
            }
        } else if (pState != blockstate) {
            pLevel.setBlock(pPos, blockstate, 3);
        }
    }

    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockPos blockpos = pContext.getClickedPos();
        Level level = pContext.getLevel();
        int i = getDistance(level, blockpos);
        return this.defaultBlockState().setValue(WATERLOGGED, level.getFluidState(blockpos).getType() == Fluids.WATER).setValue(DISTANCE, i).setValue(BOTTOM, i == 0);
    }

    public static int getDistance(BlockGetter pLevel, BlockPos pPos) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable().move(Direction.DOWN);
        BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
        int i = STABILITY_MAX_DISTANCE;
        if (blockstate.is(RadiocraftBlocks.ANTENNA_POLE.get())) {
            i = Math.min(i, blockstate.getValue(DISTANCE) + 1);
        } else if (blockstate.isFaceSturdy(pLevel, blockpos$mutableblockpos, Direction.UP)) {
            return 0;
        }

        return i;
    }

}
