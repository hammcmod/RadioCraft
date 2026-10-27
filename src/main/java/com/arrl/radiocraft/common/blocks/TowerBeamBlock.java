package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A steel lattice tower leg: a beam that joins every tower beam touching it, through a face, an edge or a corner, so
 * a column of them is a tower leg and a 2 by 2 of columns braces itself. There are no blockstate properties (26
 * neighbours would make 2^26 states): the model ({@code client.model.TowerBeamModel}) and the shape both read the
 * neighbours. Three thicknesses (8, 6 and 4 pixels square) join each other.
 */
public class TowerBeamBlock extends Block {

    /** The 26 neighbours, in a fixed order: the bit of each in {@link #connections}. */
    public static final List<Offset> OFFSETS = offsets();

    /** One neighbour: its offset and the name of the arm that reaches it ({@code up_north_east}, {@code west}, ...). */
    public record Offset(int dx, int dy, int dz, String name) {}

    private final int width;
    private final VoxelShape core;
    private final VoxelShape[] faceArms = new VoxelShape[6];

    /** @param width The beam's thickness in pixels (even, at most 16). */
    public TowerBeamBlock(Properties properties, int width) {
        super(properties);
        this.width = width;
        double lo = 8.0D - width / 2.0D, hi = 8.0D + width / 2.0D;
        core = Block.box(lo, lo, lo, hi, hi, hi);
        faceArms[Direction.DOWN.ordinal()] = Block.box(lo, 0.0D, lo, hi, lo, hi);
        faceArms[Direction.UP.ordinal()] = Block.box(lo, hi, lo, hi, 16.0D, hi);
        faceArms[Direction.NORTH.ordinal()] = Block.box(lo, lo, 0.0D, hi, hi, lo);
        faceArms[Direction.SOUTH.ordinal()] = Block.box(lo, lo, hi, hi, hi, 16.0D);
        faceArms[Direction.WEST.ordinal()] = Block.box(0.0D, lo, lo, lo, hi, hi);
        faceArms[Direction.EAST.ordinal()] = Block.box(hi, lo, lo, 16.0D, hi, hi);
    }

    public int getWidth() {
        return width;
    }

    /** Bit {@code i} set when the neighbour at {@code OFFSETS.get(i)} is a tower beam. */
    public static int connections(BlockGetter level, BlockPos pos) {
        int mask = 0;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for(int i = 0; i < OFFSETS.size(); i++) {
            Offset offset = OFFSETS.get(i);
            at.setWithOffset(pos, offset.dx(), offset.dy(), offset.dz());
            if(level.getBlockState(at).getBlock() instanceof TowerBeamBlock)
                mask |= 1 << i;
        }
        return mask;
    }

    /** The core, and an arm out of each face that meets another beam; the diagonal braces are left to walk through. */
    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        VoxelShape shape = core;
        boolean any = false;
        for(Direction direction : Direction.values()) {
            if(level.getBlockState(pos.relative(direction)).getBlock() instanceof TowerBeamBlock) {
                shape = Shapes.or(shape, faceArms[direction.ordinal()]);
                any = true;
            }
        }
        if(!any) // On its own it is drawn as a straight post, as in the hand.
            shape = Shapes.or(shape, faceArms[Direction.UP.ordinal()], faceArms[Direction.DOWN.ordinal()]);
        return shape;
    }

    @Override
    protected boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return 1.0F;
    }

    private static List<Offset> offsets() {
        List<Offset> out = new ArrayList<>();
        for(int dy : new int[] { 0, 1, -1 })
            for(int dz : new int[] { 0, -1, 1 })
                for(int dx : new int[] { 0, 1, -1 }) {
                    if(dx == 0 && dy == 0 && dz == 0)
                        continue;
                    List<String> parts = new ArrayList<>();
                    if(dy != 0)
                        parts.add(dy > 0 ? "up" : "down");
                    if(dz != 0)
                        parts.add(dz < 0 ? "north" : "south");
                    if(dx != 0)
                        parts.add(dx > 0 ? "east" : "west");
                    out.add(new Offset(dx, dy, dz, String.join("_", parts).toLowerCase(Locale.ROOT)));
                }
        return List.copyOf(out);
    }
}
