package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.capabilities.BENetworksCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractNetworkBlock extends BaseEntityBlock {

	protected AbstractNetworkBlock(Properties properties) {
		super(properties);
	}

	// Connecting to adjacent wires is not done in onPlace: vanilla calls it before the block entity exists, so there is
	// no network object to connect yet. It happens when the object is created instead, see
	// INetworkObjectProvider#getNetworkObject and WireUtils#connectToWires.

	/**
	 * Takes the network object at {@code pos} off its networks and out of the level's list: for blocks that are not
	 * network blocks by family but put an object on a wire (the H19, the direction finder), from their onRemove.
	 */
	public static void leaveNetworks(Level level, BlockPos pos) {
		if(level.isClientSide)
			return;
		IBENetworks cap = BENetworksCapability.get(level);
		BENetworkObject networkObject = cap.getObject(pos);
		if(networkObject != null)
			networkObject.clearNetworks();
		cap.removeObject(pos);
	}

	@Override
	public void onRemove(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
		if(!level.isClientSide) {
			if(newState.getBlock() != this) {
				// The block capability is keyed by the block at pos, and the new block is already in the chunk when
				// onRemove runs, so go to the level's capability directly or nothing would ever be cleaned up.
				IBENetworks cap = BENetworksCapability.get(level);
				BENetworkObject networkObject = cap.getObject(pos);
				if(networkObject != null)
					networkObject.clearNetworks(); // Tells the other members (radio, antenna) this device is gone.
				cap.removeObject(pos); // Discards it: an antenna also comes off the air here.
			}
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
		return RenderShape.MODEL;
	}

	/*
	 * Note about the code below -
	 * VoxelShape contains functionality for rotating the shape based on the direction of the block.
	 * Unfortunately, their implementation in both VoxelShape and Shapes culls sides that don't touch a face. :(
	 * The below code was written by Claude 4 Sonnet, so if there's an error, that might be why.
	 */

	private double[] getShapeDimensions(@NotNull VoxelShape shape) {
		var boxes = shape.toAabbs().getFirst();
		return new double[]{
				boxes.minX * 16, boxes.minZ * 16,
				boxes.maxX * 16, boxes.maxZ * 16
		};
	}

	/**
	 * Rotates the given voxel shape on the horizontal plane from the model direction to the target direction.
	 *
	 * @param modelDirection the {@link Direction} that the base shape is currently oriented towards in the model
	 * @param targetDirection the {@link Direction} to which the shape should be rotated
	 * @param baseShape the original {@link VoxelShape} to be rotated
	 * @return the rotated {@link VoxelShape} after applying the specified transformation
	 */
	protected @NotNull VoxelShape rotateHorizontalPlaneDirection(@NotNull Direction modelDirection, @NotNull Direction targetDirection, @NotNull VoxelShape baseShape) {
		// If model and target directions are the same, no rotation needed
		if (modelDirection == targetDirection) {
			return baseShape;
		}

		// Calculate the rotation needed (in 90-degree increments clockwise)
		int rotationSteps = getRotationSteps(modelDirection, targetDirection);

		VoxelShape result = baseShape;
		for (int i = 0; i < rotationSteps; i++) {
			result = rotateClockwise90(result);
		}

		return result;
	}

	/**
	 * Calculate the number of 90-degree clockwise rotation steps needed to go from model to the target direction
	 */
	private int getRotationSteps(Direction modelDirection, Direction targetDirection) {
		int modelIndex = getDirectionIndex(modelDirection);
		int targetIndex = getDirectionIndex(targetDirection);

		// Calculate clockwise steps needed
		return (targetIndex - modelIndex + 4) % 4;
	}

	/**
	 * Convert a direction to index for rotation calculation
	 * SOUTH=0, EAST=1, NORTH=2, WEST=3 (clockwise order)
	 */
	private int getDirectionIndex(Direction direction) {
		return switch (direction) {
			case SOUTH -> 0;
			case EAST -> 1;
			case NORTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
	}

	/**
	 * Rotate a shape 90 degrees clockwise around the center point (8, 8)
	 */
	private VoxelShape rotateClockwise90(VoxelShape shape) {
		double[] dims = getShapeDimensions(shape);
		double minX = dims[0];
		double minZ = dims[1];
		double maxX = dims[2];
		double maxZ = dims[3];

		// 90° clockwise rotation: (x, z) -> (z, 16-x)
		double newMinZ = 16.0D - maxX;
		double newMaxZ = 16.0D - minX;

		return Block.box(minZ, 0.0D, newMinZ, maxZ, 16.0D, newMaxZ);
	}
}
