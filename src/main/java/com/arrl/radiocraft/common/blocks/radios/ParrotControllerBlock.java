package com.arrl.radiocraft.common.blocks.radios;

import com.arrl.radiocraft.common.blockentities.ParrotControllerBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A small desk unit that turns the radio next to it into a simplex "parrot" repeater: whatever that radio receives is
 * sent out again through the radio's own antenna after a delay. One player can build two stations, put a parrot on the
 * far one and hear their own voice or morse come back at the strength the path gives it.
 * <p>
 * The block links itself to the first radio it finds on a horizontal side ({@link #LINK} points at it and a short wire
 * is drawn towards it, like coax does). The green LED lights while the radio is receiving, the red one while the echo
 * is being sent. Right click cycles the delay.
 */
public class ParrotControllerBlock extends BaseEntityBlock {

	public static final MapCodec<ParrotControllerBlock> CODEC = simpleCodec(ParrotControllerBlock::new);

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	/** Side the linked radio is on, {@link LinkSide#NONE} when no radio is adjacent. Absolute, not relative to FACING. */
	public static final EnumProperty<LinkSide> LINK = EnumProperty.create("link", LinkSide.class);
	public static final BooleanProperty RECEIVING = BooleanProperty.create("receiving");
	public static final BooleanProperty TRANSMITTING = BooleanProperty.create("transmitting");

	// The model is 12 wide, 3 tall and 10 deep with the LEDs on its north face; turned east/west the footprint swaps.
	private static final VoxelShape SHAPE_NS = Block.box(2.0D, 0.0D, 3.0D, 14.0D, 3.0D, 13.0D);
	private static final VoxelShape SHAPE_EW = Block.box(3.0D, 0.0D, 2.0D, 13.0D, 3.0D, 14.0D);

	@SuppressWarnings("this-escape")
	public ParrotControllerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState()
				.setValue(FACING, Direction.NORTH)
				.setValue(LINK, LinkSide.NONE)
				.setValue(RECEIVING, false)
				.setValue(TRANSMITTING, false));
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(FACING, LINK, RECEIVING, TRANSMITTING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		return state.setValue(LINK, findRadio(context.getLevel(), context.getClickedPos(), LinkSide.NONE));
	}

	/**
	 * Keeps {@link #LINK} pointing at a radio as neighbours come and go. Vanilla calls this for the changed side only,
	 * so the link is recomputed from the whole neighbourhood: the current radio is kept if it is still there.
	 */
	@Override
	public @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState,
										   @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
		if(direction.getAxis().isHorizontal())
			return state.setValue(LINK, findRadio(level, pos, state.getValue(LINK)));
		return state;
	}

	/** The side of {@code pos} with a radio on it, preferring {@code current} if that radio is still there. */
	public static LinkSide findRadio(BlockGetter level, BlockPos pos, LinkSide current) {
		if(current != LinkSide.NONE && isRadio(level, pos.relative(current.direction())))
			return current;
		for(Direction direction : Direction.Plane.HORIZONTAL)
			if(isRadio(level, pos.relative(direction)))
				return LinkSide.of(direction);
		return LinkSide.NONE;
	}

	private static boolean isRadio(BlockGetter level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() instanceof RadioBlock;
	}

	@Override
	public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
	}

	@Override
	public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new ParrotControllerBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, RadiocraftBlockEntities.PARROT_CONTROLLER.get(), ParrotControllerBlockEntity::tick);
	}

	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
		if(!level.isClientSide && level.getBlockEntity(pos) instanceof ParrotControllerBlockEntity be) {
			int delay = be.cycleDelay();
			player.displayClientMessage(Component.translatable("message.radiocraft.parrot_controller.delay", delay / 20.0D), true);
			level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4F, 0.8F); // A small front-panel button.
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	// A comparator against the unit reads the delay in seconds (1 to 15).

	@Override
	protected boolean hasAnalogOutputSignal(@NotNull BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
		return level.getBlockEntity(pos) instanceof ParrotControllerBlockEntity be ? be.getComparatorOutput() : 0;
	}

	@Override
	public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
		if(!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof ParrotControllerBlockEntity be)
			be.release(); // Stop the radio parroting once its controller is gone.
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
		tooltipComponents.add(Component.translatable("tooltip.radiocraft.parrot_controller"));
	}

	/** Horizontal side the controller's wire runs to, or none. */
	public enum LinkSide implements StringRepresentable {
		NONE("none", null),
		NORTH("north", Direction.NORTH),
		EAST("east", Direction.EAST),
		SOUTH("south", Direction.SOUTH),
		WEST("west", Direction.WEST);

		private final String name;
		private final Direction direction;

		LinkSide(String name, Direction direction) {
			this.name = name;
			this.direction = direction;
		}

		public static LinkSide of(Direction direction) {
			return switch(direction) {
				case NORTH -> NORTH;
				case EAST -> EAST;
				case SOUTH -> SOUTH;
				case WEST -> WEST;
				default -> NONE;
			};
		}

		/** The direction, or null for {@link #NONE}. */
		@Nullable
		public Direction direction() {
			return direction;
		}

		@Override
		public @NotNull String getSerializedName() {
			return name;
		}
	}
}
