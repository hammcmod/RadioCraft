package com.arrl.radiocraft.common.blocks.antennas;

import com.arrl.radiocraft.common.blockentities.AntennaSwitchBlockEntity;
import com.arrl.radiocraft.common.blocks.AbstractNetworkBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The antenna switch (a Workman CX-3 style coax switch): one radio, three antennas, one at a time. The input, the
 * brass connector, faces the radio's coax by itself (see {@link AntennaSwitchBlockEntity#faceTheRadio}); outputs 1, 2
 * and 3 are the other sides, clockwise from it. Right click turns the knob to the next output. Model and texture from
 * {@code tools/antenna_switch_model.py}.
 */
public class AntennaSwitchBlock extends AbstractNetworkBlock {

	/** The input side: where the radio's coax comes in. */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** The output the knob is turned to: 1, 2 or 3, clockwise from the input. */
	public static final IntegerProperty OUTPUT = IntegerProperty.create("output", 1, 3);
	/** The 10x6x10 box; the connectors reach the block's sides at floor level. */
	public static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 6.0D, 13.0D);

	public AntennaSwitchBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OUTPUT, 1));
	}

	/** The side the radio's coax comes in on. */
	public static Direction input(BlockState state) {
		return state.getValue(FACING);
	}

	/** The side of the selected output: {@link #OUTPUT} quarter turns clockwise from the input. */
	public static Direction output(BlockState state) {
		Direction side = state.getValue(FACING);
		for(int i = 0; i < state.getValue(OUTPUT); i++)
			side = side.getClockWise();
		return side;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OUTPUT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction toward = context.getHorizontalDirection().getOpposite(); // Input toward the player, until a radio is found.
		return defaultBlockState().setValue(FACING, AntennaSwitchBlockEntity.radioSide(context.getLevel(), context.getClickedPos(), toward));
	}

	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
		if(level instanceof ServerLevel serverLevel) {
			BlockState turned = state.setValue(OUTPUT, state.getValue(OUTPUT) % 3 + 1);
			level.setBlock(pos, turned, Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, 1.4F);
			AntennaSwitchBlockEntity.rewire(serverLevel, pos);
			player.displayClientMessage(Component.translatable("message.radiocraft.antenna_switch", turned.getValue(OUTPUT)), true);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
		super.neighborChanged(state, level, pos, block, fromPos, isMoving);
		if(level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof AntennaSwitchBlockEntity entity)
			entity.faceTheRadio(serverLevel); // Coax laid up to it: turn to the radio straight away.
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
		return new AntennaSwitchBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, RadiocraftBlockEntities.ANTENNA_SWITCH.get(), AntennaSwitchBlockEntity::tick);
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("tooltip.radiocraft.antenna_switch"));
	}
}
