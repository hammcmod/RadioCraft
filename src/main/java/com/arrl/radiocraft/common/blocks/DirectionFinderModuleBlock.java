package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.DirectionFinderModuleBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * A direction finder module: one antenna input of a direction finder, taking coax at the back. Stack three to eight
 * against a direction finder (or against each other) and give each the same kind of antenna. Its lamp is green
 * while locked on, red without the right antenna.
 */
public class DirectionFinderModuleBlock extends AbstractNetworkBlock {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<Lamp> LAMP = EnumProperty.create("lamp", Lamp.class);
	private static final Map<Direction, VoxelShape> SHAPES = SpeakerBlock.shapes(new double[] { 0.0D, 0.0D, 2.75D, 16.0D, 14.0D, 14.0D });

	public enum Lamp implements StringRepresentable {
		OFF, RED, GREEN;

		@Override
		public @NotNull String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	@SuppressWarnings("this-escape")
	public DirectionFinderModuleBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LAMP, Lamp.OFF));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
		builder.add(FACING, LAMP);
	}

	@Override
	public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
		return InteractionResult.PASS;
	}

	@Override
	protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return simpleCodec(DirectionFinderModuleBlock::new);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new DirectionFinderModuleBlockEntity(pos, state);
	}

	@Override
	public void appendHoverText(@NotNull net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.@NotNull TooltipContext context,
								@NotNull java.util.List<net.minecraft.network.chat.Component> tooltip, @NotNull net.minecraft.world.item.TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.radiocraft.direction_finder_module").withStyle(net.minecraft.ChatFormatting.GRAY));
	}
}
