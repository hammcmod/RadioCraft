package com.arrl.radiocraft.common.blocks.radios;

import com.arrl.radiocraft.common.blockentities.CommercialRepeaterBlockEntity;
import com.arrl.radiocraft.common.blocks.StackedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The commercial repeater: an open silver rack four blocks tall, from the bottom its batteries and charger, the repeater
 * radio and amplifier, the duplexer cavities, and the controller under the roof. It works as a VHF repeater with its
 * duplexer and power built in, and runs hotter ({@link CommercialRepeaterBlockEntity}). Not craftable: found at
 * repeater sites. Placed and broken as one; any cell opens it.
 */
public class CommercialRepeaterBlock extends VHFRepeaterBlock {

	/** The radio's front panel lamps (cell 1) and the beacon on the controller (cell 3). */
	public static final BooleanProperty LAMP_PWR = BooleanProperty.create("pwr");
	public static final BooleanProperty LAMP_RX = BooleanProperty.create("rx");
	public static final BooleanProperty LAMP_TX = BooleanProperty.create("tx");

	@SuppressWarnings("this-escape")
	public CommercialRepeaterBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(StackedBlocks.PART, 0).setValue(LAMP_PWR, false).setValue(LAMP_RX, false).setValue(LAMP_TX, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(StackedBlocks.PART, LAMP_PWR, LAMP_RX, LAMP_TX);
	}

	@Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return Shapes.block();
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null || !StackedBlocks.hasRoom(context, StackedBlocks.HEIGHT) ? null : state.setValue(CEILING, false);
	}

	@Override
	public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		StackedBlocks.placeAbove(level, pos, state, StackedBlocks.PART, StackedBlocks.HEIGHT);
	}

	@Override
	public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbour, @NotNull LevelAccessor level,
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

	/** Any cell opens the repeater, which lives in the bottom one. */
	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
		BlockPos bottom = StackedBlocks.bottom(pos, state, StackedBlocks.PART);
		return super.useWithoutItem(level.getBlockState(bottom), level, bottom, player, hitResult);
	}

	@Override
	protected int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
		return super.getAnalogOutputSignal(state, level, StackedBlocks.bottom(pos, state, StackedBlocks.PART));
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return state.getValue(StackedBlocks.PART) == 0 ? new CommercialRepeaterBlockEntity(pos, state) : null;
	}
}
