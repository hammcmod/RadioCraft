package com.arrl.radiocraft.common.blocks.radios;

import com.arrl.radiocraft.common.blockentities.DigitalInterfaceBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
 * The digital interface (TNC). It takes the callsign of whoever places it, works through the radios on any side of it,
 * beacons on a redstone pulse, and its comparator output is 15 while a message waits to be read. Right clicking it
 * with paper, books or maps (or a hopper) stocks its printer.
 */
public class DigitalInterfaceBlock extends BaseEntityBlock {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final HashMap<Direction, VoxelShape> SHAPES = new HashMap<>();

	static {
		SHAPES.put(Direction.NORTH, Block.box(0.0D, 0.0D, 4.0D, 16.0D, 6.0D, 16.0D));
		SHAPES.put(Direction.SOUTH, Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 12.0D));
		SHAPES.put(Direction.EAST, Block.box(0.0D, 0.0D, 0.0D, 12.0D, 6.0D, 16.0D));
		SHAPES.put(Direction.WEST, Block.box(4.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D));
	}

	public DigitalInterfaceBlock(Properties properties) {
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
		if(placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof DigitalInterfaceBlockEntity tnc)
			tnc.claim(player);
	}

	/** Paper, books and maps in hand go into the interface's supply instead of opening it. */
	@Override
	protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
	                                                   @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
		if(!(level.getBlockEntity(pos) instanceof DigitalInterfaceBlockEntity tnc))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		boolean supply = false;
		for(int slot = 0; slot < tnc.getSupplies().getSlots(); slot++)
			supply |= DigitalInterfaceBlockEntity.isSupply(slot, stack);
		if(!supply)
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if(!level.isClientSide) {
			ItemStack left = tnc.insertSupply(stack.copy());
			if(!player.getAbilities().instabuild)
				stack.setCount(left.getCount());
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
		if(!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DigitalInterfaceBlockEntity tnc)
			tnc.dropSupplies();
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
		if(!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof DigitalInterfaceBlockEntity tnc)
			serverPlayer.openMenu(tnc, pos);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected boolean hasAnalogOutputSignal(@NotNull BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(@NotNull BlockState state, Level level, @NotNull BlockPos pos) {
		return level.getBlockEntity(pos) instanceof DigitalInterfaceBlockEntity tnc ? tnc.getComparatorOutput() : 0;
	}

	@Override
	public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return DigitalInterfaceBlockEntity::tick;
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return null;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new DigitalInterfaceBlockEntity(pos, state);
	}
}
