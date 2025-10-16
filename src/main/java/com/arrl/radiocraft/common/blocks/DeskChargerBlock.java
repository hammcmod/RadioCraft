package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.DeskChargerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.level.Level;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;

/**
 * Desk Charger block that hosts a Geo BlockEntity for rendering the geckolib model.
 */
public class DeskChargerBlock extends BaseEntityBlock {
    public static final MapCodec<DeskChargerBlock> CODEC = simpleCodec(DeskChargerBlock::new);

    public DeskChargerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // Split into base (always present) and radio (only when item present in slot) shapes
    private static final VoxelShape BASE_SHAPE = makeBaseShape();
    private static final VoxelShape RADIO_SHAPE = makeRadioShape();

    private static VoxelShape makeBaseShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0.359375, 0.09375, 0.5624998582892072, 0.640625, 0.125, 0.609375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.34375, 0, 0.40625, 0.65625, 0.09375, 0.625), BooleanOp.OR);

        return shape;
    }

    private static VoxelShape makeRadioShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0.375, 0.03125, 0.4375, 0.625, 0.40625, 0.59375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.390625, 0.40625, 0.53125, 0.4375, 0.671875, 0.578125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.359375, 0.3125, 0.4921875, 0.40625, 0.359375, 0.5390625), BooleanOp.OR);

        return shape;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // If there's a DeskChargerBlockEntity with a radio in the slot, include the radio shape
        if (level.getBlockEntity(pos) instanceof DeskChargerBlockEntity desk) {
            var stack = desk.inventory.getStackInSlot(0);
            if (stack != null && !stack.isEmpty()) {
                return Shapes.or(BASE_SHAPE, RADIO_SHAPE);
            }
        }
        return BASE_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof DeskChargerBlockEntity desk) {
            var stack = desk.inventory.getStackInSlot(0);
            if (stack != null && !stack.isEmpty()) {
                return Shapes.or(BASE_SHAPE, RADIO_SHAPE);
            }
        }
        return BASE_SHAPE;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof DeskChargerBlockEntity desk) {
            var stack = desk.inventory.getStackInSlot(0);
            if (stack != null && !stack.isEmpty()) {
                return Shapes.or(BASE_SHAPE, RADIO_SHAPE);
            }
        }
        return BASE_SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeskChargerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof DeskChargerBlockEntity desk) {
                DeskChargerBlockEntity.tick(lvl, pos, st, desk);
            }
        };
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(state.getMenuProvider(level, pos), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof DeskChargerBlockEntity desk) {
                desk.drops();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
