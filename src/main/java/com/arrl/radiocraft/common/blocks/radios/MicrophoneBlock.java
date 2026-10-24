package com.arrl.radiocraft.common.blocks.radios;

import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blocks.RadioLinks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The desk microphone (maintainer's models, 2026-10-23): a small mic standing at the front of the block, on the side
 * nearest its radio ({@link #SIDE}, left or right as seen from the front). With no radio beside it, it stays put.
 */
public final class MicrophoneBlock extends Block {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<Side> SIDE = EnumProperty.create("side", Side.class);
	/** Outlines by side and facing; the models face north, with the left mic at x 11 to 15 and the right at 1 to 5. */
	private static final Map<Side, Map<Direction, VoxelShape>> SHAPES = new EnumMap<>(Side.class);

	static {
		for(Side side : Side.values()) {
			double x1 = side == Side.LEFT ? 11.0D : 1.0D, x2 = x1 + 4.0D, z1 = 0.0D, z2 = 6.0D;
			Map<Direction, VoxelShape> byFacing = new EnumMap<>(Direction.class);
			for(Direction facing : Direction.Plane.HORIZONTAL) {
				double ax = x1, bx = x2, az = z1, bz = z2;
				int turns = switch(facing) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
				for(int i = 0; i < turns; i++) { // 90 degrees clockwise seen from above: (x, z) -> (16 - z, x)
					double nax = 16 - bz, nbx = 16 - az, naz = ax, nbz = bx;
					ax = nax; bx = nbx; az = naz; bz = nbz;
				}
				byFacing.put(facing, Block.box(ax, 0.0D, az, bx, 9.0D, bz));
			}
			SHAPES.put(side, byFacing);
		}
	}

	/** Which side of the block the mic stands on, as seen from its front. */
	public enum Side implements StringRepresentable {
		LEFT, RIGHT;

		@Override
		public @NotNull String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public MicrophoneBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(FACING, SIDE);
	}

	/** The side seen from the front as the player's left: facing north, the player stands north looking south, left is east. */
	private static Direction left(BlockState state) {
		return state.getValue(FACING).getClockWise();
	}

	/** The side with a radio on it, left first; {@code current} when there is none. */
	private static Side sideFor(BlockState state, LevelReader level, BlockPos pos) {
		if(RadioLinks.RADIO.test(level.getBlockState(pos.relative(left(state)))))
			return Side.LEFT;
		if(RadioLinks.RADIO.test(level.getBlockState(pos.relative(left(state).getOpposite()))))
			return Side.RIGHT;
		return state.getValue(SIDE);
	}

	/** The radio this mic stands beside, on its {@link #SIDE}, or null. */
	@Nullable
	public static RadioBlockEntity radio(BlockState state, BlockGetter level, BlockPos pos) {
		Direction side = state.getValue(SIDE) == Side.LEFT ? left(state) : left(state).getOpposite();
		return level.getBlockEntity(pos.relative(side)) instanceof RadioBlockEntity radio ? radio : null;
	}

	/**
	 * Hold right click to talk through the radio beside it; sneak and right click to lock talking on (and again to let
	 * go). The mic hears whoever is close: further away, quieter. It lets go by itself after desk_mic_timeout_seconds.
	 */
	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
		RadioBlockEntity radio = radio(state, level, pos);
		if(radio == null)
			return InteractionResult.PASS;
		if(!level.isClientSide) {
			if(player.isShiftKeyDown()) {
				boolean locked = radio.toggleDeskMicLock(player, pos);
				player.displayClientMessage(Component.translatable(locked ? "message.radiocraft.desk_mic.locked" : "message.radiocraft.desk_mic.unlocked",
						RadiocraftServerConfig.DESK_MIC_TIMEOUT_SECONDS.get()), true);
			}
			else
				radio.pressDeskMic(player, pos);
			level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4F, 1.4F);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		return state.setValue(SIDE, sideFor(state, context.getLevel(), context.getClickedPos()));
	}

	/** Moves the mic over to a radio placed beside it. */
	@Override
	protected @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbour, @NotNull LevelAccessor level,
	                                          @NotNull BlockPos pos, @NotNull BlockPos neighbourPos) {
		BlockState updated = super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
		return direction.getAxis().isHorizontal() ? updated.setValue(SIDE, sideFor(updated, level, pos)) : updated;
	}

	@Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPES.get(state.getValue(SIDE)).get(state.getValue(FACING));
	}

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.radiocraft.microphone"));
    }
}
