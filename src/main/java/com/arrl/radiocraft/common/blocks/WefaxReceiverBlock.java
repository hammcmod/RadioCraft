package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.WefaxReceiverBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A WEFAX receiver (model by the maintainer, 2026-10-23): with a radio beside it, it prints weather charts and the solar
 * report ({@link WefaxReceiverBlockEntity}). Use paper on it to load it; use it with an empty hand for its controls.
 */
public class WefaxReceiverBlock extends BaseEntityBlock {

	public static final MapCodec<WefaxReceiverBlock> CODEC = simpleCodec(WefaxReceiverBlock::new);
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	/** A printed sheet waits in the tray (the model shows it sticking out of the front) until someone takes it. */
	public static final net.minecraft.world.level.block.state.properties.BooleanProperty READY = net.minecraft.world.level.block.state.properties.BooleanProperty.create("ready");
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		// The model faces north: a full 3 px base, and the printer body from z 2 back, 8 high.
		VoxelShape base = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D);
		SHAPES.put(Direction.NORTH, Shapes.or(base, Block.box(0.0D, 3.0D, 2.0D, 16.0D, 8.0D, 16.0D)));
		SHAPES.put(Direction.SOUTH, Shapes.or(base, Block.box(0.0D, 3.0D, 0.0D, 16.0D, 8.0D, 14.0D)));
		SHAPES.put(Direction.EAST, Shapes.or(base, Block.box(0.0D, 3.0D, 0.0D, 14.0D, 8.0D, 16.0D)));
		SHAPES.put(Direction.WEST, Shapes.or(base, Block.box(2.0D, 3.0D, 0.0D, 16.0D, 8.0D, 16.0D)));
	}

	@SuppressWarnings("this-escape")
	public WefaxReceiverBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(READY, false));
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, READY);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(READY, false);
	}

	@Override
	protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	/** Paper goes into the supply. */
	@Override
	protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player,
													   @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
		if(!stack.is(Items.PAPER) || !(level.getBlockEntity(pos) instanceof WefaxReceiverBlockEntity receiver))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if(!level.isClientSide) {
			ItemStack left = receiver.insertPaper(stack.copy());
			if(left.getCount() == stack.getCount())
				player.displayClientMessage(Component.translatable("screen.radiocraft.wefax.paper_full"), true);
			else if(!player.getAbilities().instabuild)
				stack.setCount(left.getCount());
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
		if(!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof WefaxReceiverBlockEntity receiver)
			serverPlayer.openMenu(receiver, pos);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston) {
		if(!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof WefaxReceiverBlockEntity receiver)
			receiver.dropContents();
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new WefaxReceiverBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, RadiocraftBlockEntities.WEFAX_RECEIVER.get(), WefaxReceiverBlockEntity::tick);
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
		tooltipComponents.add(Component.translatable("tooltip.radiocraft.wefax_receiver").withStyle(ChatFormatting.GRAY));
	}
}
