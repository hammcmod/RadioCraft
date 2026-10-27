package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.TelephoneBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.items.TelephoneHandsetItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The telephone (see {@link TelephoneBlockEntity}), on a desk or, placed against the side of a block, on the wall.
 * Right click opens the keypad. Sneak-right-click with an empty hand picks up the handset on its cord (answering a
 * ringing phone); sneak-right-click with the handset puts it back, ending the call. {@link #OFF_HOOK} hides the
 * handset on the model while it is up.
 */
public class TelephoneBlock extends AbstractNetworkBlock {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	/** Mounted on the side of a block, facing away from it. */
	public static final BooleanProperty WALL = BooleanProperty.create("wall");
	/** The handset is out in someone's hand. */
	public static final BooleanProperty OFF_HOOK = BooleanProperty.create("off_hook");
	private static final Map<Direction, VoxelShape> DESK_SHAPES = SpeakerBlock.shapes(new double[] { 4.0D, 0.0D, 2.0D, 12.2D, 3.9D, 11.5D });
	/** Facing north the wall is to the south: the phone and its backplate, as tools/telephone_models.py prints them. */
	private static final Map<Direction, VoxelShape> WALL_SHAPES = wallShapes();

	private static Map<Direction, VoxelShape> wallShapes() {
		Map<Direction, VoxelShape> body = SpeakerBlock.shapes(new double[] { 4.0D, 2.0D, 12.0D, 12.0D, 11.5D, 15.75D });
		Map<Direction, VoxelShape> plate = SpeakerBlock.shapes(new double[] { 0.0D, 0.0D, 15.75D, 16.0D, 16.0D, 16.0D });
		Map<Direction, VoxelShape> out = new java.util.EnumMap<>(Direction.class);
		for(Direction direction : body.keySet())
			out.put(direction, net.minecraft.world.phys.shapes.Shapes.or(body.get(direction), plate.get(direction)));
		return out;
	}

	@SuppressWarnings("this-escape")
	public TelephoneBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WALL, false).setValue(OFF_HOOK, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
		builder.add(FACING, WALL, OFF_HOOK);
	}

	/** On the side of a block it goes on the wall, facing out; otherwise on the desk, facing the player. */
	@Override
	public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		if(face.getAxis().isHorizontal())
			return defaultBlockState().setValue(FACING, face).setValue(WALL, true);
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** Sneak-right-click with an empty hand: the handset comes up on its cord. Otherwise the keypad opens. */
	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
		if(!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof TelephoneBlockEntity phone) {
			if(player.isShiftKeyDown())
				phone.pickUp(serverPlayer);
			else
				serverPlayer.openMenu(phone, pos);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	/** With its own handset: sneak-right-click hangs it back up, a plain right click opens the keypad to dial. */
	@Override
	protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
		if(level.getBlockEntity(pos) instanceof TelephoneBlockEntity phone && TelephoneHandsetItem.isHandsetOf(stack, phone)) {
			if(!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
				if(player.isShiftKeyDown())
					phone.putDown(serverPlayer);
				else
					serverPlayer.openMenu(phone, pos);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return (state.getValue(WALL) ? WALL_SHAPES : DESK_SHAPES).get(state.getValue(FACING));
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return simpleCodec(TelephoneBlock::new);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new TelephoneBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return level.isClientSide || type != RadiocraftBlockEntities.TELEPHONE.get() ? null : TelephoneBlockEntity::tick;
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("tooltip.radiocraft.telephone").withStyle(ChatFormatting.GRAY));
	}
}
