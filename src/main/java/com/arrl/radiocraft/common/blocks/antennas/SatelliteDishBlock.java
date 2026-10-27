package com.arrl.radiocraft.common.blocks.antennas;

import com.arrl.radiocraft.common.blockentities.MicrowaveDishBlockEntity;
import com.arrl.radiocraft.common.blockentities.SatelliteDishBlockEntity;
import com.arrl.radiocraft.common.blocks.AbstractNetworkBlock;
import com.arrl.radiocraft.common.radio.DirectionFinding;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The microwave dish (GeckoLib model, with its placement animation): one end of a microwave link, on the coax of a
 * microwave radio. It points exactly where the player looked when placing it, up and down included
 * ({@link MicrowaveDishBlockEntity}); {@link #FACING} is the nearest of the four directions, for anything that only
 * understands those.
 */
public class SatelliteDishBlock extends AbstractNetworkBlock {
    public static final MapCodec<SatelliteDishBlock> CODEC = simpleCodec(SatelliteDishBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    @SuppressWarnings("this-escape")
    public SatelliteDishBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); // Facing the player, as the dish does.
    }

    /**
     * Turns the dish to face the player who placed it, level, as a furnace's front does: stand toward the far end and
     * place it, and it points roughly there. Not tilted by where they look: a player placing a block looks down at it,
     * which would aim every new dish into the ground. Peak it on the radio's ALIGN page.
     */
    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if(placer != null && level.getBlockEntity(pos) instanceof MicrowaveDishBlockEntity dish) {
            dish.setAzimuth(DirectionFinding.normalize(placer.getYRot())); // Yaw 0 looks south, so the dish faces back north at them.
            dish.setElevation(0.0D);
        }
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL; // GeckoLib draws it from the block entity.
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new SatelliteDishBlockEntity(pos, state);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.radiocraft.microwave_dish").withStyle(ChatFormatting.GRAY));
    }
}
