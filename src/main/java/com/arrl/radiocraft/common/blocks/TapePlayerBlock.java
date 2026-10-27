package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.TapePlayerBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.items.TapeItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The reel-to-reel tape player ({@link TapePlayerBlockEntity}). Use it with a tape reel to load it, with an empty hand
 * to open its controls, sneaking with an empty hand to take the tape out. Its reels turn while it plays or records.
 */
public class TapePlayerBlock extends BaseEntityBlock {

	public static final MapCodec<TapePlayerBlock> CODEC = simpleCodec(TapePlayerBlock::new);
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	/** Playing or recording: the reels turn. */
	public static final BooleanProperty RUNNING = BooleanProperty.create("running");

	@SuppressWarnings("this-escape")
	public TapePlayerBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RUNNING, false));
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
		builder.add(FACING, RUNNING);
	}

	@Override
	protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player,
													   @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
		if(!(stack.getItem() instanceof TapeItem) || !(level.getBlockEntity(pos) instanceof TapePlayerBlockEntity tapePlayer))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if(!tapePlayer.getTape().isEmpty())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION; // Full: open the controls instead.
		if(!level.isClientSide && tapePlayer.insert(stack))
			stack.consume(1, player);
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
		if(!(level.getBlockEntity(pos) instanceof TapePlayerBlockEntity tapePlayer))
			return InteractionResult.PASS;
		if(!level.isClientSide) {
			if(player.isShiftKeyDown()) {
				ItemStack tape = tapePlayer.eject();
				if(!tape.isEmpty() && !player.getInventory().add(tape))
					player.drop(tape, false);
			}
			else if(player instanceof ServerPlayer serverPlayer)
				serverPlayer.openMenu(tapePlayer, pos);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston) {
		if(!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TapePlayerBlockEntity tapePlayer) {
			ItemStack tape = tapePlayer.eject();
			if(!tape.isEmpty())
				Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, tape);
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new TapePlayerBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return createTickerHelper(type, RadiocraftBlockEntities.TAPE_PLAYER.get(),
				level.isClientSide ? TapePlayerBlockEntity::clientTick : TapePlayerBlockEntity::tick);
	}
}
