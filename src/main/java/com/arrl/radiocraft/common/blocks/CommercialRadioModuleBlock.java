package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.CommercialRadioModuleBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The commercial radio module: a GE MASTR II style cabinet, four blocks tall (louvred door, badge and handle, the
 * control panel with its lamps, the vent and cap), sealed: an all-band transmitter nobody can set, sending one program
 * on one frequency ({@link CommercialRadioModuleBlockEntity}). Found in structures; broken, it gives up one rare part
 * (see the block loot provider) rather than itself.
 */
public class CommercialRadioModuleBlock extends BaseEntityBlock {

	public static final MapCodec<CommercialRadioModuleBlock> CODEC = simpleCodec(CommercialRadioModuleBlock::new);
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	/** The control panel's lamps (cell 2). */
	public static final BooleanProperty LAMP_PWR = BooleanProperty.create("pwr");
	public static final BooleanProperty LAMP_RX = BooleanProperty.create("rx");
	public static final BooleanProperty LAMP_TX = BooleanProperty.create("tx");

	@SuppressWarnings("this-escape")
	public CommercialRadioModuleBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(StackedBlocks.PART, 0)
				.setValue(LAMP_PWR, false).setValue(LAMP_RX, false).setValue(LAMP_TX, false));
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
		builder.add(FACING, StackedBlocks.PART, LAMP_PWR, LAMP_RX, LAMP_TX);
	}

	@Override
	protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return Shapes.block();
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
		return StackedBlocks.hasRoom(context, StackedBlocks.HEIGHT) ? defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()) : null;
	}

	@Override
	public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		StackedBlocks.placeAbove(level, pos, state, StackedBlocks.PART, StackedBlocks.HEIGHT);
	}

	@Override
	protected @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbour, @NotNull LevelAccessor level,
											  @NotNull BlockPos pos, @NotNull BlockPos neighbourPos) {
		if(StackedBlocks.brokenApart(state, direction, neighbour, StackedBlocks.PART, StackedBlocks.HEIGHT))
			return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
	}

	@Override
	protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
		return StackedBlocks.canSurvive(state, level, pos, StackedBlocks.PART);
	}

	@Override
	public @NotNull BlockState playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
		StackedBlocks.creativeBreak(level, pos, state, player, StackedBlocks.PART);
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** Sealed: there is nothing to set on it. In creative, any cell opens its setup (for building structures). */
	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
		if(level.isClientSide)
			return InteractionResult.SUCCESS;
		BlockPos bottom = StackedBlocks.bottom(pos, state, StackedBlocks.PART);
		if(player.isCreative() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
				&& level.getBlockEntity(bottom) instanceof CommercialRadioModuleBlockEntity module)
			serverPlayer.openMenu(module, bottom);
		else
			player.displayClientMessage(Component.translatable("block.radiocraft.commercial_radio_module.sealed"), true);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return state.getValue(StackedBlocks.PART) == 0 ? new CommercialRadioModuleBlockEntity(pos, state) : null;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return level.isClientSide || state.getValue(StackedBlocks.PART) != 0 ? null
				: createTickerHelper(type, RadiocraftBlockEntities.COMMERCIAL_RADIO_MODULE.get(), CommercialRadioModuleBlockEntity::tick);
	}
}
