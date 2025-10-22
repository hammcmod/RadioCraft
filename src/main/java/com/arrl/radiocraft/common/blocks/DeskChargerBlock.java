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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;
import java.util.Objects;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import java.util.EnumMap;
import java.util.Map;

/**
 * Desk Charger block with energy storage and directional placement.
 * <p>
 * Features:
 * <ul>
 *   <li>Charges VHF Handheld radios placed in its inventory slot</li>
 *   <li>Dynamic collision shapes that adjust when a radio is present</li>
 *   <li>Horizontal directional placement (rotates to face away from player)</li>
 *   <li>Pre-computed VoxelShapes for optimal performance</li>
 *   <li>GeckoLib-based rendering with dynamic LED indicators</li>
 * </ul>
 */
public class DeskChargerBlock extends BaseEntityBlock {
    public static final MapCodec<DeskChargerBlock> CODEC = simpleCodec(DeskChargerBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** Base charger shape for NORTH orientation (y=180° in blockstate) */
    private static final VoxelShape BASE_SHAPE_NORTH = makeBaseShape();
    
    /** Radio shape for NORTH orientation (only used when radio is present) */
    private static final VoxelShape RADIO_SHAPE_NORTH = makeRadioShape();
    
    /** Combined charger + radio shape for NORTH orientation */
    private static final VoxelShape COMBINED_SHAPE_NORTH = Shapes.or(BASE_SHAPE_NORTH, RADIO_SHAPE_NORTH);

    /** Pre-computed base shapes cached by direction for performance */
    private static final Map<Direction, VoxelShape> BASE_SHAPES = new EnumMap<>(Direction.class);
    
    /** Pre-computed combined shapes cached by direction for performance */
    private static final Map<Direction, VoxelShape> COMBINED_SHAPES = new EnumMap<>(Direction.class);

    static {
        // Pre-calculate all rotations at class load time to avoid runtime computation
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BASE_SHAPES.put(direction, calculateShapeForDirection(BASE_SHAPE_NORTH, direction));
            COMBINED_SHAPES.put(direction, calculateShapeForDirection(COMBINED_SHAPE_NORTH, direction));
        }
    }

    public DeskChargerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * Creates the base VoxelShape for the charger (without radio).
     * Defines the collision bounds for the charger unit itself.
     *
     * @return VoxelShape representing the charger base
     */
    private static VoxelShape makeBaseShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0.359375, 0.09375, 0.5624998582892072, 0.640625, 0.125, 0.609375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.34375, 0, 0.40625, 0.65625, 0.09375, 0.625), BooleanOp.OR);
        return shape;
    }

    /**
     * Creates the VoxelShape for the radio when placed in the charger.
     * Defines collision bounds for the VHF Handheld radio model.
     *
     * @return VoxelShape representing the radio
     */
    private static VoxelShape makeRadioShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0.375, 0.03125, 0.4375, 0.625, 0.40625, 0.59375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.390625, 0.40625, 0.53125, 0.4375, 0.671875, 0.578125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.359375, 0.3125, 0.4921875, 0.40625, 0.359375, 0.5390625), BooleanOp.OR);
        return shape;
    }

    /**
     * Calculates the rotated shape for a given horizontal direction.
     * Applies 90° clockwise rotations based on direction ordinal.
     * <p>
     * Direction mapping:
     * <ul>
     *   <li>NORTH (ordinal 2): 0 rotations</li>
     *   <li>EAST (ordinal 3): 1 rotation (90° CW)</li>
     *   <li>SOUTH (ordinal 0): 2 rotations (180°)</li>
     *   <li>WEST (ordinal 1): 3 rotations (270° CW)</li>
     * </ul>
     *
     * @param baseShape The shape in NORTH orientation to rotate
     * @param direction Target horizontal direction
     * @return Rotated VoxelShape for the specified direction
     */
    private static VoxelShape calculateShapeForDirection(VoxelShape baseShape, Direction direction) {
        // Calculate number of 90° clockwise rotations needed
        // NORTH(2) = 0 rotations, EAST(3) = 1, SOUTH(0) = 2, WEST(1) = 3
        int rotations = (direction.get2DDataValue() - Direction.NORTH.get2DDataValue() + 4) % 4;
        
        VoxelShape result = baseShape;
        for (int i = 0; i < rotations; i++) {
            result = rotateShapeClockwise(result);
        }
        return result;
    }

    /**
     * Rotates a VoxelShape 90 degrees clockwise around the Y-axis (viewed from above).
     * <p>
     * Transformation formula: (x, z) → (1-z, x)
     * <ul>
     *   <li>newMinX = 1 - oldMaxZ</li>
     *   <li>newMaxX = 1 - oldMinZ</li>
     *   <li>newMinZ = oldMinX</li>
     *   <li>newMaxZ = oldMaxX</li>
     *   <li>Y coordinates remain unchanged</li>
     * </ul>
     *
     * @param shape The shape to rotate
     * @return New VoxelShape rotated 90° clockwise
     */
    private static VoxelShape rotateShapeClockwise(VoxelShape shape) {
        VoxelShape rotated = Shapes.empty();
        for (AABB aabb : shape.toAabbs()) {
            // 90° clockwise: (x, z) -> (1-z, x)
            double newMinX = 1.0 - aabb.maxZ;
            double newMinZ = aabb.minX;
            double newMaxX = 1.0 - aabb.minZ;
            double newMaxZ = aabb.maxX;
            rotated = Shapes.join(rotated, Shapes.box(newMinX, aabb.minY, newMinZ, newMaxX, aabb.maxY, newMaxZ), BooleanOp.OR);
        }
        return rotated;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }

    /**
     * Determines the appropriate VoxelShape based on block state and inventory contents.
     * Returns combined shape (charger + radio) if a radio is present in the inventory slot,
     * otherwise returns base shape (charger only).
     * <p>
     * Shapes are pre-computed and cached per direction for optimal performance.
     *
     * @param state Block state (used to get FACING direction)
     * @param level Block getter for accessing block entity
     * @param pos Block position
     * @return VoxelShape for collision/rendering (base or combined)
     */
    private VoxelShape getShapeForState(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        
        // Check if radio is present and return combined shape, otherwise just base
        if (level.getBlockEntity(pos) instanceof DeskChargerBlockEntity desk) {
            var radioStack = desk.inventory.getStackInSlot(0);
            if (radioStack != null && !radioStack.isEmpty()) {
                return COMBINED_SHAPES.get(facing);
            }
        }
        
        return BASE_SHAPES.get(facing);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShapeForState(state, level, pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShapeForState(state, level, pos);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return getShapeForState(state, level, pos);
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

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.radiocraft.desk_charger"));
    }
}
