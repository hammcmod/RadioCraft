package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.RadioInterfaceBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

/**
 * The radio interface (TNC). It takes the callsign of whoever places it and works through the radios on any side of
 * it; APRS and file transfer terminals joined to it use them. See {@link RadioInterfaceBlockEntity}.
 */
public class RadioInterfaceBlock extends BaseEntityBlock {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final HashMap<Direction, VoxelShape> SHAPES = new HashMap<>();

	static {
		SHAPES.put(Direction.NORTH, Block.box(0.0D, 0.0D, 4.0D, 16.0D, 6.0D, 16.0D));
		SHAPES.put(Direction.SOUTH, Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 12.0D));
		SHAPES.put(Direction.EAST, Block.box(0.0D, 0.0D, 0.0D, 12.0D, 6.0D, 16.0D));
		SHAPES.put(Direction.WEST, Block.box(4.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D));
	}

	public RadioInterfaceBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if(placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof RadioInterfaceBlockEntity radioInterface)
			radioInterface.claim(player);
	}

	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
		if(!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof RadioInterfaceBlockEntity radioInterface)
			serverPlayer.openMenu(radioInterface, pos);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return RadioInterfaceBlockEntity::tick;
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return simpleCodec(RadioInterfaceBlock::new);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new RadioInterfaceBlockEntity(pos, state);
	}
}
