package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.WeatherStationBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A mast-top weather station (see {@link WeatherStationBlockEntity}): anemometer cups that spin with the wind, a vane
 * that turns into it (both drawn by the block entity's renderer) and a radiation shield. Like a wireless console, any H19 within {@link WeatherStationBlockEntity#RANGE} blocks
 * beacons its weather. Right click for the current reading.
 */
public class WeatherStationBlock extends BaseEntityBlock {

	private static final VoxelShape SHAPE = Block.box(0.5, 0, 1.25, 14.75, 15.5, 14.75);

	public WeatherStationBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
		if(level instanceof ServerLevel && level.getBlockEntity(pos) instanceof WeatherStationBlockEntity station)
			player.displayClientMessage(Component.literal(station.describe()), false);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return simpleCodec(WeatherStationBlock::new);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new WeatherStationBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		if(type != RadiocraftBlockEntities.WEATHER_STATION.get())
			return null;
		return level.isClientSide ? (lvl, pos, st, be) -> WeatherStationBlockEntity.clientTick(lvl, pos, st, (WeatherStationBlockEntity)be)
				: WeatherStationBlockEntity::tick;
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("tooltip.radiocraft.weather_station").withStyle(ChatFormatting.GRAY));
	}
}
