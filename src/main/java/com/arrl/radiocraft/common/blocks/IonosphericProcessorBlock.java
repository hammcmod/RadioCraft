package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.IonosphericProcessorBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Ionospheric Processor (maintainer's model, 2026-10-23): a rack two blocks tall with three glowing valves on top
 * that light the room a little ({@link #VALVE_LIGHT}), placed and broken as one. With an Ionosonde Antenna on the coax
 * of its lower half it measures the sky and broadcasts solar bulletins for WEFAX receivers through a radio it touches
 * (see {@link IonosphericProcessorBlockEntity}). Right click for its terminal; a comparator reads the conditions.
 */
public class IonosphericProcessorBlock extends AbstractNetworkBlock {

	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	/** Light the valves give off, from the top half: a soft glow around the rack, about half a torch. */
	public static final int VALVE_LIGHT = 7;

	public IonosphericProcessorBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HALF, FACING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		if(pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context))
			return null;
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(HALF, DoubleBlockHalf.LOWER);
	}

	@Override
	public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
		level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
		if(placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof IonosphericProcessorBlockEntity processor)
			processor.claim(player); // Bulletins go out under the placer's callsign.
	}

	/** The block entity, on the lower half, from either half. */
	@Nullable
	private static IonosphericProcessorBlockEntity processorAt(Level level, BlockPos pos, BlockState state) {
		BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
		return level.getBlockEntity(lower) instanceof IonosphericProcessorBlockEntity processor ? processor : null;
	}

	/** Right click: its terminal (and it takes the clicker's callsign if it has none). */
	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
		if(!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
			IonosphericProcessorBlockEntity processor = processorAt(level, pos, state);
			if(processor != null) {
				processor.claim(serverPlayer);
				serverPlayer.openMenu(processor, processor.getBlockPos());
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected boolean hasAnalogOutputSignal(@NotNull BlockState state) {
		return true;
	}

	/** 1 (blackout) to 6 (great) while its ionosonde is on its coax, from either half. */
	@Override
	protected int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
		IonosphericProcessorBlockEntity processor = processorAt(level, pos, state);
		return processor == null ? 0 : processor.getConditionLevel();
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return simpleCodec(IonosphericProcessorBlock::new);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new IonosphericProcessorBlockEntity(pos, state) : null;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, RadiocraftBlockEntities.IONOSPHERIC_PROCESSOR.get(), IonosphericProcessorBlockEntity::tick);
	}

	/** Either half goes when the other does, like a door. */
	@Override
	protected @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighbour, @NotNull LevelAccessor level,
	                                          @NotNull BlockPos pos, @NotNull BlockPos neighbourPos) {
		DoubleBlockHalf half = state.getValue(HALF);
		if(direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP))
			return neighbour.is(this) && neighbour.getValue(HALF) != half ? state : Blocks.AIR.defaultBlockState();
		return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
	}

	@Override
	protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
		if(state.getValue(HALF) == DoubleBlockHalf.LOWER)
			return true;
		BlockState below = level.getBlockState(pos.below());
		return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
	}

	/** In creative, breaking the top half must not let the bottom half drop the item. */
	@Override
	public @NotNull BlockState playerWillDestroy(Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
		if(!level.isClientSide && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
			BlockPos below = pos.below();
			BlockState belowState = level.getBlockState(below);
			if(belowState.is(this) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER) {
				level.setBlock(below, belowState.getFluidState().is(Fluids.WATER) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 35);
				level.levelEvent(player, 2001, below, Block.getId(belowState));
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
		tooltipComponents.add(Component.translatable("tooltip.radiocraft.ionospheric_processor").withStyle(ChatFormatting.GRAY));
	}
}
