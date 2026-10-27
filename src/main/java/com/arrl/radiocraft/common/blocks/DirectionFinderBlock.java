package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.DirectionFinderBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * The direction finder's terminal (see {@link DirectionFinderBlockEntity}): the plain one, which plots a fix as a
 * point in a box, or the map enabled one (made by crafting a world map module into a plain one), which plots it on a
 * map. Its screen shows {@link Display}.
 */
public class DirectionFinderBlock extends BaseEntityBlock {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<Display> DISPLAY = EnumProperty.create("display", Display.class);
	/** The model's bounds facing north. */
	private static final Map<Direction, VoxelShape> SHAPES = SpeakerBlock.shapes(new double[] { 0.0D, 0.0D, 2.5D, 16.0D, 14.0D, 16.0D });

	/** What the screen on the block shows: dark, the amber prompt, or the compass once it has a bearing. */
	public enum Display implements StringRepresentable {
		OFF, ON, COMPASS;

		@Override
		public @NotNull String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private final boolean map;

	@SuppressWarnings("this-escape")
	public DirectionFinderBlock(Properties properties, boolean map) {
		super(properties);
		this.map = map;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DISPLAY, Display.OFF));
	}

	/** True for the map enabled direction finder. */
	public boolean hasMap() {
		return map;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
		builder.add(FACING, DISPLAY);
	}

	@Override
	public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
		if(!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof DirectionFinderBlockEntity finder)
			serverPlayer.openMenu(finder, pos);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
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

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return simpleCodec(properties -> new DirectionFinderBlock(properties, map));
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new DirectionFinderBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return level.isClientSide || type != RadiocraftBlockEntities.DIRECTION_FINDER.get() ? null : DirectionFinderBlockEntity::tick;
	}

	@Override
	public void appendHoverText(@NotNull net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.@NotNull TooltipContext context,
								@NotNull java.util.List<net.minecraft.network.chat.Component> tooltip, @NotNull net.minecraft.world.item.TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(net.minecraft.network.chat.Component.translatable(map ? "tooltip.radiocraft.map_direction_finder" : "tooltip.radiocraft.direction_finder").withStyle(net.minecraft.ChatFormatting.GRAY));
	}
}
