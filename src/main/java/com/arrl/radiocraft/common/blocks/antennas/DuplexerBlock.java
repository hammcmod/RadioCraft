package com.arrl.radiocraft.common.blocks.antennas;

import com.arrl.radiocraft.common.blockentities.DuplexerBlockEntity;
import com.arrl.radiocraft.common.blocks.power.AbstractPowerNetworkBlock;
import com.mojang.serialization.MapCodec;
import com.arrl.radiocraft.common.blocks.radios.RadioBlock;
import com.arrl.radiocraft.common.blocks.radios.VHFRepeaterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The duplexer. On top of a repeater it shows its coax running down to the repeater's back ({@link #ON_REPEATER}) and
 * turns to face it, so the cables land there.
 */
public class DuplexerBlock extends AbstractPowerNetworkBlock {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty ON_REPEATER = BooleanProperty.create("on_repeater");
	public static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 3.0D, 14.0D);

	public DuplexerBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(FACING, ON_REPEATER);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		return onRepeater(state, context.getLevel().getBlockState(context.getClickedPos().below()));
	}

	/** Sets {@link #ON_REPEATER} from the block below; on a repeater, faces its back so the coax runs down it. */
	private static BlockState onRepeater(BlockState state, BlockState below) {
		if(below.getBlock() instanceof VHFRepeaterBlock)
			return state.setValue(ON_REPEATER, true).setValue(FACING, below.getValue(RadioBlock.HORIZONTAL_FACING).getOpposite());
		return state.setValue(ON_REPEATER, false);
	}

	@Override
	protected @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState,
	                                          @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
		BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
		return direction == Direction.DOWN && updated.is(this) ? onRepeater(updated, neighborState) : updated;
	}

	@Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return null;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new DuplexerBlockEntity(pos, state);
	}
}
