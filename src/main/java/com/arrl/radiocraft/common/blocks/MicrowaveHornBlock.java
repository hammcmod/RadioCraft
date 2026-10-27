package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The AT&T Long Lines microwave horn-reflector: two by two blocks and four tall, cut into 16 parts
 * ({@code microwave_horn_<x>_<y>_<z>}, built by {@code tools/build_models.py}). Placing the item puts the whole horn
 * up with its mouth facing where the player looks, the clicked block being the back left of the bottom layer;
 * breaking any part takes the whole horn down and drops one horn. The placed part (bottom layer, back left) takes the
 * coax from a microwave radio and holds where the horn points ({@link com.arrl.radiocraft.common.blockentities.MicrowaveDishBlockEntity}):
 * one end of a microwave link with {@code microwave_horn_range_factor} times a dish's range.
 */
public class MicrowaveHornBlock extends Block implements net.minecraft.world.level.block.EntityBlock {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final IntegerProperty PART_X = IntegerProperty.create("part_x", 0, 1);
	public static final IntegerProperty PART_Y = IntegerProperty.create("part_y", 0, 3);
	public static final IntegerProperty PART_Z = IntegerProperty.create("part_z", 0, 1);
	public static final int SIZE_X = 2, SIZE_Y = 4, SIZE_Z = 2;
	/** The part the player places, which carries the drop: bottom layer, back row (the mouth faces model north). */
	public static final int ORIGIN_X = 0, ORIGIN_Y = 0, ORIGIN_Z = 1;

	/**
	 * The solid parts of each part's model, clamped to its block, facing north: {minX, minY, minZ, maxX, maxY, maxZ}
	 * in pixels, by {@code x * 8 + y * 2 + z}. From the cut models; rotated pieces count as their unrotated box.
	 */
	private static final double[][][] BOXES = {
			{{10, 0, 10, 16, 1, 16}, {14, 1, 14, 16, 6, 16}, {13, 6, 13, 16, 10, 16}, {11.5, 10, 11.5, 16, 14, 16}, {10, 14, 10, 16, 16, 16}}, // 0 0 0
			{{10, 0, 0, 16, 1, 6}, {14, 1, 0, 16, 6, 2}, {13, 6, 0, 16, 10, 3}, {11.5, 10, 0, 16, 14, 4.5}, {10, 14, 0, 16, 16, 6}}, // 0 0 1
			{{10, 0, 10, 16, 2, 16}, {8.5, 2, 8.5, 16, 6, 16}, {7, 6, 7, 16, 10, 16}, {5.5, 10, 5.5, 16, 14, 16}, {4, 14, 4, 16, 16, 16}}, // 0 1 0
			{{10, 0, 0, 16, 2, 6}, {8.5, 2, 0, 16, 6, 7.5}, {7, 6, 0, 16, 10, 9}, {5.5, 10, 0, 16, 14, 10.5}, {4, 14, 0, 16, 16, 12}}, // 0 1 1
			{{4, 0, 4, 16, 2, 16}, {2.5, 2, 2.5, 16, 6, 16}, {2, 6, 2, 4, 16, 4}, {2, 6, 4, 4, 16, 6}, {2, 6, 6, 4, 16, 8}, {2, 6, 8, 4, 16, 10}, {2, 6, 10, 4, 16, 12}, {2, 6, 12, 4, 16, 14}, {2, 6, 14, 4, 16, 16}, {4, 6, 2, 16, 16, 3}, {2, 5, 1.5, 16, 6, 3}}, // 0 2 0
			{{4, 0, 0, 16, 2, 12}, {2.5, 2, 0, 16, 6, 13.5}, {4, 6, 14, 16, 10.4, 15.5}, {2, 6, 0, 4, 16, 2}, {2, 6, 2, 4, 16, 4}, {2, 6, 4, 4, 16, 6}, {2, 6, 6, 4, 16, 8}, {2, 6, 8, 4, 16, 10}, {2, 6, 10, 4, 16, 12}, {2, 6, 12, 4, 16, 14}}, // 0 2 1
			{{4, 14.5, 2, 16, 16, 9.9}, {2, 0, 2, 4, 16, 4}, {2, 0, 4, 4, 16, 6}, {2, 0, 6, 4, 16, 8}, {2, 0, 8, 4, 16, 10}, {2, 0, 10, 4, 15.9, 12}, {2, 0, 12, 4, 15.1, 14}, {2, 0, 14, 4, 14.3, 16}, {4, 0, 2, 16, 14.5, 3}, {2, 14.5, 1.5, 16, 16, 3}}, // 0 3 0
			{{2, 0, 0, 4, 13.5, 2}, {2, 0, 2, 4, 12.6, 4}, {2, 0, 4, 4, 11.3, 6}, {2, 0, 6, 4, 9.3, 8}, {2, 0, 8, 4, 7.3, 10}, {2, 0, 10, 4, 5.3, 12}, {2, 0, 12, 4, 0.7, 14}}, // 0 3 1
			{{0, 0, 10, 6, 1, 16}, {0, 1, 14, 2, 6, 16}, {0, 6, 13, 3, 10, 16}, {0, 10, 11.5, 4.5, 14, 16}, {0, 14, 10, 6, 16, 16}}, // 1 0 0
			{{0, 0, 0, 6, 1, 6}, {0, 1, 0, 2, 6, 2}, {0, 6, 0, 3, 10, 3}, {0, 10, 0, 4.5, 14, 4.5}, {0, 14, 0, 6, 16, 6}}, // 1 0 1
			{{0, 0, 10, 6, 2, 16}, {0, 2, 8.5, 7.5, 6, 16}, {0, 6, 7, 9, 10, 16}, {0, 10, 5.5, 10.5, 14, 16}, {0, 14, 4, 12, 16, 16}}, // 1 1 0
			{{0, 0, 0, 6, 2, 6}, {0, 2, 0, 7.5, 6, 7.5}, {0, 6, 0, 9, 10, 9}, {0, 10, 0, 10.5, 14, 10.5}, {0, 14, 0, 12, 16, 12}}, // 1 1 1
			{{0, 0, 4, 12, 2, 16}, {0, 2, 2.5, 13.5, 6, 16}, {12, 6, 2, 14, 16, 4}, {12, 6, 4, 14, 16, 6}, {12, 6, 6, 14, 16, 8}, {12, 6, 8, 14, 16, 10}, {12, 6, 10, 14, 16, 12}, {12, 6, 12, 14, 16, 14}, {12, 6, 14, 14, 16, 16}, {0, 6, 2, 12, 16, 3}, {0, 5, 1.5, 14, 6, 3}}, // 1 2 0
			{{0, 0, 0, 12, 2, 12}, {0, 2, 0, 13.5, 6, 13.5}, {0, 6, 14, 12, 10.4, 15.5}, {0, 10.4, 14, 12, 16, 15.5}, {12, 6, 0, 14, 16, 2}, {12, 6, 2, 14, 16, 4}, {12, 6, 4, 14, 16, 6}, {12, 6, 6, 14, 16, 8}, {12, 6, 8, 14, 16, 10}, {12, 6, 10, 14, 16, 12}, {12, 6, 12, 14, 16, 14}}, // 1 2 1
			{{0, 10.7, 8.6, 12, 12.2, 16}, {0, 14.5, 2, 12, 16, 9.9}, {12, 0, 2, 14, 16, 4}, {12, 0, 4, 14, 16, 6}, {12, 0, 6, 14, 16, 8}, {12, 0, 8, 14, 16, 10}, {12, 0, 10, 14, 15.9, 12}, {12, 0, 12, 14, 15.1, 14}, {12, 0, 14, 14, 14.3, 16}, {0, 0, 2, 12, 14.5, 3}, {0, 14.5, 1.5, 14, 16, 3}}, // 1 3 0
			{{0, 3.6, 0, 12, 5.1, 10.2}, {12, 0, 0, 14, 13.5, 2}, {12, 0, 2, 14, 12.6, 4}, {12, 0, 4, 14, 11.3, 6}, {12, 0, 6, 14, 9.3, 8}, {12, 0, 8, 14, 7.3, 10}, {12, 0, 10, 14, 5.3, 12}, {12, 0, 12, 14, 0.7, 14}}, // 1 3 1
	};
	private static final Map<Direction, VoxelShape[]> SHAPES = new EnumMap<>(Direction.class);

	static {
		for(Direction facing : Direction.Plane.HORIZONTAL) {
			VoxelShape[] shapes = new VoxelShape[BOXES.length];
			for(int i = 0; i < BOXES.length; i++) {
				VoxelShape shape = Shapes.empty();
				for(double[] box : BOXES[i])
					shape = Shapes.or(shape, rotated(box, facing));
				shapes[i] = shape.isEmpty() ? Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D) : shape.optimize();
			}
			SHAPES.put(facing, shapes);
		}
	}

	@SuppressWarnings("this-escape")
	public MicrowaveHornBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
				.setValue(PART_X, ORIGIN_X).setValue(PART_Y, ORIGIN_Y).setValue(PART_Z, ORIGIN_Z));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
		builder.add(FACING, PART_X, PART_Y, PART_Z);
	}

	/** A box drawn facing north, turned to face {@code facing} about the block's centre. */
	private static VoxelShape rotated(double[] b, Direction facing) {
		return switch(facing) {
			case EAST -> Block.box(16.0D - b[5], b[1], b[0], 16.0D - b[2], b[4], b[3]);
			case SOUTH -> Block.box(16.0D - b[3], b[1], 16.0D - b[5], 16.0D - b[0], b[4], 16.0D - b[2]);
			case WEST -> Block.box(b[2], b[1], 16.0D - b[3], b[5], b[4], 16.0D - b[0]);
			default -> Block.box(b[0], b[1], b[2], b[3], b[4], b[5]);
		};
	}

	/** Where part {@code (x, y, z)} of a horn facing {@code facing} sits, from where its origin part sits. */
	public static BlockPos partPos(BlockPos origin, Direction facing, int x, int y, int z) {
		int vx = x - ORIGIN_X, vz = z - ORIGIN_Z; // In the model, north is -z.
		int dx, dz;
		switch(facing) {
			case EAST -> { dx = -vz; dz = vx; }
			case SOUTH -> { dx = -vx; dz = -vz; }
			case WEST -> { dx = vz; dz = -vx; }
			default -> { dx = vx; dz = vz; }
		}
		return origin.offset(dx, y - ORIGIN_Y, dz);
	}

	/** The origin part's position for the part in {@code state} at {@code pos}. */
	public static BlockPos originOf(BlockState state, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockPos fromOrigin = partPos(BlockPos.ZERO, facing, state.getValue(PART_X), state.getValue(PART_Y), state.getValue(PART_Z));
		return pos.subtract(fromOrigin);
	}

	/** Every part's position and state for a horn whose origin part is at {@code origin}. */
	public List<Map.Entry<BlockPos, BlockState>> parts(BlockPos origin, Direction facing) {
		List<Map.Entry<BlockPos, BlockState>> out = new ArrayList<>();
		for(int x = 0; x < SIZE_X; x++)
			for(int y = 0; y < SIZE_Y; y++)
				for(int z = 0; z < SIZE_Z; z++)
					out.add(Map.entry(partPos(origin, facing, x, y, z), defaultBlockState().setValue(FACING, facing)
							.setValue(PART_X, x).setValue(PART_Y, y).setValue(PART_Z, z)));
		return out;
	}

	public static boolean isOrigin(BlockState state) {
		return state.getValue(PART_X) == ORIGIN_X && state.getValue(PART_Y) == ORIGIN_Y && state.getValue(PART_Z) == ORIGIN_Z;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection();
		BlockPos origin = context.getClickedPos();
		Level level = context.getLevel();
		for(Map.Entry<BlockPos, BlockState> part : parts(origin, facing)) {
			BlockPos at = part.getKey();
			if(at.equals(origin))
				continue;
			if(!level.isInWorldBounds(at) || !level.getBlockState(at).canBeReplaced(context))
				return null; // No room for the whole horn.
		}
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if(level.isClientSide)
			return;
		for(Map.Entry<BlockPos, BlockState> part : parts(pos, state.getValue(FACING)))
			if(!part.getKey().equals(pos))
				level.setBlock(part.getKey(), part.getValue(), Block.UPDATE_ALL);
	}

	/** In creative, the origin goes first and quietly, so taking the horn down drops nothing. */
	@Override
	public @NotNull BlockState playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
		if(!level.isClientSide && player.isCreative() && !isOrigin(state)) {
			BlockPos origin = originOf(state, pos);
			BlockState there = level.getBlockState(origin);
			if(there.is(this) && isOrigin(there))
				level.setBlock(origin, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/**
	 * One part gone takes the rest with it. The origin carries the drop (see the loot table), so when another part goes
	 * first the origin is broken as if mined; the others just vanish.
	 */
	@Override
	protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston) {
		if(!level.isClientSide && isOrigin(state) && !newState.is(this)) { // Off the coax, as AbstractNetworkBlock does.
			com.arrl.radiocraft.api.capabilities.IBENetworks cap = com.arrl.radiocraft.common.capabilities.BENetworksCapability.get(level);
			com.arrl.radiocraft.api.benetworks.BENetworkObject networkObject = cap.getObject(pos);
			if(networkObject != null)
				networkObject.clearNetworks();
			cap.removeObject(pos);
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
		if(level.isClientSide || newState.is(this))
			return;
		BlockPos origin = originOf(state, pos);
		Direction facing = state.getValue(FACING);
		for(Map.Entry<BlockPos, BlockState> part : parts(origin, facing)) {
			BlockPos at = part.getKey();
			if(at.equals(pos) || !level.getBlockState(at).equals(part.getValue()))
				continue;
			if(isOrigin(part.getValue()))
				level.destroyBlock(at, true);
			else
				level.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
		}
	}

	/** Only the placed part has a block entity: it is the horn's end of the link. */
	@Override
	public net.minecraft.world.level.block.entity.@Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return isOrigin(state) ? com.arrl.radiocraft.common.blockentities.MicrowaveDishBlockEntity.horn(pos, state) : null;
	}

	@Override
	protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPES.get(state.getValue(FACING))[state.getValue(PART_X) * 8 + state.getValue(PART_Y) * 2 + state.getValue(PART_Z)];
	}

	@Override
	protected boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
		return true;
	}
}
