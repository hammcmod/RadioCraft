package com.arrl.radiocraft.common.blocks;

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
public class SolarWeatherStationBlock extends BaseEntityBlock {

	public static final MapCodec<SolarWeatherStationBlock> CODEC = simpleCodec(SolarWeatherStationBlock::new);
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public SolarWeatherStationBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
		if(!level.isClientSide && level.getBlockEntity(pos) instanceof SolarWeatherStationBlockEntity be)
			player.displayClientMessage(be.getReport(), false);
		return InteractionResult.sidedSuccess(level.isClientSide);
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
