package com.arrl.radiocraft.common.blocks;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import com.arrl.radiocraft.common.blockentities.AprsTerminalBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * The APRS terminal, an H19 (model by the maintainer, 2026-10-23). Its screen shows {@link Display}; opening it puts
 * out CONTACT and MSG. Beacons on a redstone pulse; comparator output 15 while a message waits to be read. See
 * {@link AprsTerminalBlockEntity}.
 */
public final class AprsTerminalBlock extends BaseEntityBlock {

	public static final MapCodec<AprsTerminalBlock> CODEC = simpleCodec(AprsTerminalBlock::new);
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<Display> DISPLAY = EnumProperty.create("display", Display.class);
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		// The model faces north: the keyboard in front (z 0 to 6, a pixel high), the screen and case behind, 10 high.
		SHAPES.put(Direction.NORTH, Shapes.or(Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.2D, 6.0D), Block.box(0.0D, 0.0D, 6.0D, 16.0D, 10.0D, 16.0D)));
		SHAPES.put(Direction.SOUTH, Shapes.or(Block.box(0.0D, 0.0D, 10.0D, 16.0D, 1.2D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 10.0D)));
		SHAPES.put(Direction.EAST, Shapes.or(Block.box(10.0D, 0.0D, 0.0D, 16.0D, 1.2D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 10.0D, 10.0D, 16.0D)));
		SHAPES.put(Direction.WEST, Shapes.or(Block.box(0.0D, 0.0D, 0.0D, 6.0D, 1.2D, 16.0D), Block.box(6.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D)));
	}

	/** What the screen shows: nothing, READY, CONTACT (a new station heard) or MSG (a message waiting). */
	public enum Display implements StringRepresentable {
		OFF, READY, CONTACT, MSG;

		@Override
		public @NotNull String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public AprsTerminalBlock(Properties properties) {
		super(properties);
		registerDefaultState(RadioLinks.none(stateDefinition.any()));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, DISPLAY);
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

	/**
	 * Empty maps used on the terminal become APRS maps of the stations it hears: one, or sneaking, a 2 by 2 wall from 4
	 * maps or a 3 by 3 from 9 (see {@link AprsTerminalBlockEntity#createMaps}).
	 */
	@Override
	protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
	                                                   @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
		if(!stack.is(Items.MAP))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if(level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof AprsTerminalBlockEntity terminal) {
			int have = player.getAbilities().instabuild ? 9 : stack.getCount();
			int size = !player.isShiftKeyDown() ? 1 : have >= 9 ? 3 : have >= 4 ? 2 : 1;
			for(ItemStack map : terminal.createMaps(serverLevel, size))
				if(!player.getInventory().add(map))
					player.drop(map, false);
			stack.consume(size * size, player);
			level.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if(placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof AprsTerminalBlockEntity terminal)
			terminal.claimFor(player);
	}

	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
		if(!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof AprsTerminalBlockEntity terminal) {
			terminal.claimFor(serverPlayer); // The first to open it gives a station without a callsign theirs.
			serverPlayer.openMenu(terminal, pos); // First, so the screen still sees what was new and opens on it.
			terminal.markSeen();
		}
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
		return level.getBlockEntity(pos) instanceof AprsTerminalBlockEntity terminal ? terminal.getComparatorOutput() : 0;
	}

	/** Off the power wire it may be on (a microwave radio's station line). */
	@Override
	protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston) {
		if(!newState.is(this))
			AbstractNetworkBlock.leaveNetworks(level, pos);
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return AprsTerminalBlockEntity::tick;
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new AprsTerminalBlockEntity(pos, state);
	}
}
