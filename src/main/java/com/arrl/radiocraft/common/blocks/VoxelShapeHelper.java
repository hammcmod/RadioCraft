package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class VoxelShapeHelper {

    private static double[] getShapeDimensions(@NotNull VoxelShape shape) {
        var boxes = shape.toAabbs().getFirst();
        return new double[]{
                boxes.minX * 16, boxes.minY * 16, boxes.minZ * 16,
                boxes.maxX * 16, boxes.maxY * 16, boxes.maxZ * 16
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
    public static @NotNull VoxelShape rotateHorizontalPlaneDirection(@NotNull Direction modelDirection, @NotNull Direction targetDirection, @NotNull VoxelShape baseShape) {
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
    private static int getRotationSteps(Direction modelDirection, Direction targetDirection) {
        int modelIndex = getDirectionIndex(modelDirection);
        int targetIndex = getDirectionIndex(targetDirection);

        // Calculate clockwise steps needed
        return (targetIndex - modelIndex + 4) % 4;
    }

    /**
     * Convert a direction to index for rotation calculation
     * SOUTH=0, EAST=1, NORTH=2, WEST=3 (clockwise order)
     */
    private static int getDirectionIndex(Direction direction) {
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
    private static VoxelShape rotateClockwise90(VoxelShape shape) {
        double[] dims = getShapeDimensions(shape);
        double minX = dims[0];
        double minY = dims[1];
        double minZ = dims[2];
        double maxX = dims[3];
        double maxY = dims[4];
        double maxZ = dims[5];

        // 90° clockwise rotation: (x, z) -> (z, 16-x)
        double newMinZ = 16.0D - maxX;
        double newMaxZ = 16.0D - minX;

        return Block.box(minZ, minY, newMinZ, maxZ, maxY, newMaxZ);
    }
}