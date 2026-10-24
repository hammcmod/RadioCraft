package com.arrl.radiocraft.common.blocks;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.Direction;
import com.arrl.radiocraft.common.blockentities.SolarWeatherStationBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Tells the operator what the solar weather is doing. Right click reports the current conditions and whether 10m is
 * open; a comparator reads the conditions as 1 (blackout) to 6 (great). The proposal's printed paper report and
 * best-band calculator are not built yet.
 */
public final class SolarWeatherStationBlock extends BaseEntityBlock {

	public static final MapCodec<SolarWeatherStationBlock> CODEC = simpleCodec(SolarWeatherStationBlock::new);
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public SolarWeatherStationBlock(Properties properties) {
		super(properties);
		registerDefaultState(RadioLinks.none(stateDefinition.any()));
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(FACING);
		RadioLinks.addTo(builder);
	}

	/**
	 * Outline and collision following the model (authored facing north): the machine body, half a block tall, and
	 * the paper roll standing up at the back. Rotated per facing like the blockstate rotates the model.
	 */
	private static final java.util.Map<net.minecraft.core.Direction, net.minecraft.world.phys.shapes.VoxelShape> SHAPES = new java.util.EnumMap<>(net.minecraft.core.Direction.class);

	static {
		double[][] boxes = { { 0, 0, 0, 16, 8, 16 }, { 3.75, 8, 13, 13.75, 15.5, 16 } };
		for(net.minecraft.core.Direction facing : net.minecraft.core.Direction.Plane.HORIZONTAL) {
			int turns = switch(facing) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
			net.minecraft.world.phys.shapes.VoxelShape shape = net.minecraft.world.phys.shapes.Shapes.empty();
			for(double[] b : boxes) {
				double x1 = b[0], z1 = b[2], x2 = b[3], z2 = b[5];
				for(int i = 0; i < turns; i++) { // 90 degrees clockwise seen from above: (x, z) -> (16 - z, x)
					double nx1 = 16 - z2, nx2 = 16 - z1, nz1 = x1, nz2 = x2;
					x1 = nx1; x2 = nx2; z1 = nz1; z2 = nz2;
				}
				shape = net.minecraft.world.phys.shapes.Shapes.or(shape, Block.box(x1, b[1], z1, x2, b[4], z2));
			}
			SHAPES.put(facing, shape);
		}
	}

	@Override
	protected @NotNull net.minecraft.world.phys.shapes.VoxelShape getShape(@NotNull BlockState state, @NotNull net.minecraft.world.level.BlockGetter level, @NotNull BlockPos pos, @NotNull net.minecraft.world.phys.shapes.CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return RadioLinks.around(this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()),
				context.getLevel(), context.getClickedPos(), RadioLinks.RADIO);
	}

	/** Shows or hides the floor cable on a side as {@link RadioLinks} neighbours come and go. */
	@Override
	protected @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbour, @NotNull LevelAccessor level,
	                                          @NotNull BlockPos pos, @NotNull BlockPos neighbourPos) {
		return RadioLinks.update(super.updateShape(state, direction, neighbour, level, pos, neighbourPos), direction, neighbour, RadioLinks.RADIO);
	}

	@Override
	public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new SolarWeatherStationBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, RadiocraftBlockEntities.SOLAR_WEATHER_STATION.get(), SolarWeatherStationBlockEntity::tick);
	}

	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
		if(!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof SolarWeatherStationBlockEntity be)
			be.startReport(serverPlayer);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	/** A fresh station rings its bell once. */
	@Override
	protected void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if(!level.isClientSide && !oldState.is(this))
			level.playSound(null, pos, com.arrl.radiocraft.common.init.RadiocraftSoundEvents.TELETYPE_DING.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	@Override
	protected boolean hasAnalogOutputSignal(@NotNull BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
		return level.getBlockEntity(pos) instanceof SolarWeatherStationBlockEntity be ? be.getConditionLevel() : 0;
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
		tooltipComponents.add(Component.translatable("tooltip.radiocraft.solar_weather_station"));
	}
}
