package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.RadioFaxBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The radio fax machine, one per log. Right clicking it with paper, books or maps (or a hopper) stocks its blanks; a
 * hopper under it takes the copies received. See {@link RadioFaxBlockEntity}.
 */
public final class RadioFaxBlock extends BaseEntityBlock {

	public static final MapCodec<RadioFaxBlock> CODEC = simpleCodec(RadioFaxBlock::new);
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 2.0D, 15.0D, 13.0D, 14.0D);

	public RadioFaxBlock(Properties properties) {
		super(properties);
		registerDefaultState(RadioLinks.none(stateDefinition.any()));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		RadioLinks.addTo(builder);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return RadioLinks.around(defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()),
				context.getLevel(), context.getClickedPos(), RadioLinks.STATION);
	}

	/** Shows or hides the floor cable on a side as {@link RadioLinks} neighbours come and go. */
	@Override
	protected @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbour, @NotNull LevelAccessor level,
	                                          @NotNull BlockPos pos, @NotNull BlockPos neighbourPos) {
		return RadioLinks.update(super.updateShape(state, direction, neighbour, level, pos, neighbourPos), direction, neighbour, RadioLinks.STATION);
	}

	/** Paper, books and maps in hand go into the supply instead of opening it. */
	@Override
	protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
	                                                   @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
		if(!(level.getBlockEntity(pos) instanceof RadioFaxBlockEntity fax))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		boolean supply = false;
		for(int slot = 0; slot < fax.getSupplies().getSlots(); slot++)
			supply |= RadioFaxBlockEntity.isSupply(slot, stack);
		if(!supply)
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if(!level.isClientSide) {
			ItemStack left = fax.insertSupply(stack.copy());
			if(!player.getAbilities().instabuild)
				stack.setCount(left.getCount());
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
		if(!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RadioFaxBlockEntity fax)
			fax.dropContents();
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
		if(!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof RadioFaxBlockEntity fax)
			serverPlayer.openMenu(fax, pos);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return RadioFaxBlockEntity::tick;
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new RadioFaxBlockEntity(pos, state);
	}
}
