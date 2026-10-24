package com.arrl.radiocraft.common.blocks.antennas;

import com.arrl.radiocraft.common.blockentities.BandFollowingAntennaBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Hustler 5-BTV multiband vertical (maintainer's model, 2026-10-23): {@link #HEIGHT} blocks tall, placed and broken
 * as one. {@link #SEGMENT} 0 is the base, where the coax connects and the antenna lives; breaking any segment takes the
 * whole antenna down and drops it once. See {@link com.arrl.radiocraft.common.radio.antenna.types.FiveBtvAntennaType}.
 */
public final class FiveBtvAntennaBlock extends AntennaCenterBlock {

    public static final int HEIGHT = 7;
    public static final IntegerProperty SEGMENT = IntegerProperty.create("segment", 0, HEIGHT - 1);
    /** The mast, and the two segments with the trap's radials (the fifth and sixth from the bottom). */
    private static final VoxelShape MAST = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D);
    private static final VoxelShape RADIALS = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);

    public FiveBtvAntennaBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SEGMENT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if(pos.getY() + HEIGHT > level.getMaxBuildHeight())
            return null;
        for(int i = 1; i < HEIGHT; i++)
            if(!level.getBlockState(pos.above(i)).canBeReplaced(context))
                return null;
        return defaultBlockState().setValue(SEGMENT, 0);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        for(int i = 1; i < HEIGHT; i++)
            level.setBlock(pos.above(i), state.setValue(SEGMENT, i), Block.UPDATE_ALL);
    }

    /** A segment goes when the one it stands on or the one above it does, so the whole mast comes down together. */
    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbour, @NotNull LevelAccessor level,
                                           @NotNull BlockPos pos, @NotNull BlockPos neighbourPos) {
        int segment = state.getValue(SEGMENT);
        if(direction == Direction.UP && segment < HEIGHT - 1 && !(neighbour.is(this) && neighbour.getValue(SEGMENT) == segment + 1))
            return Blocks.AIR.defaultBlockState();
        if(direction == Direction.DOWN && segment > 0 && !(neighbour.is(this) && neighbour.getValue(SEGMENT) == segment - 1))
            return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        int segment = state.getValue(SEGMENT);
        if(segment == 0)
            return true;
        BlockState below = level.getBlockState(pos.below());
        return below.is(this) && below.getValue(SEGMENT) == segment - 1;
    }

    /** In creative, breaking the mast must not let the base drop the antenna as the rest comes down. */
    @Override
    public @NotNull BlockState playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
        int segment = state.getValue(SEGMENT);
        if(!level.isClientSide && player.isCreative() && segment > 0) {
            BlockPos base = pos.below(segment);
            BlockState baseState = level.getBlockState(base);
            if(baseState.is(this) && baseState.getValue(SEGMENT) == 0) {
                level.setBlock(base, baseState.getFluidState().is(Fluids.WATER) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, base, Block.getId(baseState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        int segment = state.getValue(SEGMENT);
        return segment == 4 || segment == 5 ? RADIALS : MAST;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return state.getValue(SEGMENT) == 0 ? new BandFollowingAntennaBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return level.isClientSide || state.getValue(SEGMENT) != 0 ? null : BandFollowingAntennaBlockEntity::tick;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.radiocraft.five_btv_antenna"));
    }
}
