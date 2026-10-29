package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.BooleanOp;
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
 * neighbours. Three thicknesses (8, 6 and 4 pixels square) join each other. Like iron bars, a beam also reaches out of
 * a face to a block that is solid there ({@link #attachesTo}): it stands on the ground, against a wall or under a
 * ceiling instead of stopping short in the middle of its block.
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

    /**
     * Bit {@code i} set when the neighbour at {@code OFFSETS.get(i)} is a tower beam the arm can reach: through a face
     * always; on a diagonal only when nothing stands in the blocks the brace passes between ({@link #pathClear}).
     */
    public static int connections(BlockGetter level, BlockPos pos) {
        int mask = 0;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for(int i = 0; i < OFFSETS.size(); i++) {
            Offset offset = OFFSETS.get(i);
            at.setWithOffset(pos, offset.dx(), offset.dy(), offset.dz());
            if(level.getBlockState(at).getBlock() instanceof TowerBeamBlock && pathClear(level, pos, offset))
                mask |= 1 << i;
        }
        return mask;
    }

    /**
     * Whether a diagonal brace from {@code pos} towards {@code offset} has room: it runs through the edge or corner the
     * two blocks share, so every other block around that edge (2) or corner (6) must be air or another beam. A block
     * of any kind there (a wall, a wire, a foothold, a torch) is in the way. Always true through a face.
     */
    public static boolean pathClear(BlockGetter level, BlockPos pos, Offset offset) {
        int[] d = { offset.dx(), offset.dy(), offset.dz() };
        int axes = (d[0] != 0 ? 1 : 0) | (d[1] != 0 ? 2 : 0) | (d[2] != 0 ? 4 : 0);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        // Every block offset by some but not all of the offset's steps: the ones the brace squeezes between.
        for(int subset = (axes - 1) & axes; subset != 0; subset = (subset - 1) & axes) {
            at.setWithOffset(pos, (subset & 1) != 0 ? d[0] : 0, (subset & 2) != 0 ? d[1] : 0, (subset & 4) != 0 ? d[2] : 0);
            BlockState state = level.getBlockState(at);
            if(!state.isAir() && !(state.getBlock() instanceof TowerBeamBlock))
                return false;
        }
        return true;
    }

    /** Bit of the face arm towards {@code direction} in a {@link #connections} mask. */
    public static int bit(Direction direction) {
        for(int i = 0; i < OFFSETS.size(); i++) {
            Offset offset = OFFSETS.get(i);
            if(offset.dx() == direction.getStepX() && offset.dy() == direction.getStepY() && offset.dz() == direction.getStepZ())
                return 1 << i;
        }
        throw new IllegalArgumentException(direction.toString());
    }

    /** The middle of a block, top to bottom: where an up or down arm meets what is above or below. */
    private static final VoxelShape CENTRE_COLUMN = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);

    /** Up and down: the arms of a beam with no beam beside it, a straight post as in the hand. */
    public static final int POST = bit(Direction.UP) | bit(Direction.DOWN);

    /**
     * The arms to draw: one to every touching beam ({@link #connections}), or a straight post when there is none, plus
     * a face arm to every block the beam {@link #attachesTo}.
     */
    public static int arms(BlockGetter level, BlockPos pos) {
        int mask = connections(level, pos);
        if(mask == 0)
            mask = POST;
        for(Direction direction : Direction.values())
            if(attachesTo(level, pos, direction))
                mask |= bit(direction);
        return mask;
    }

    /**
     * Whether a beam at {@code pos} reaches out of its {@code direction} face to the block there, as iron bars do: the
     * block's facing side is solid enough to bear on (a full face, a wall or fence post), and it is not one vanilla
     * keeps bars off (leaves, barriers, ...). Up and down it also reaches into an antenna or wire holder mounted on the
     * tower, and into anything whose shape meets the shared face where the beam would: a torch or lamp on top (the
     * beacon controller's light), a lantern hung under it. Never for a beam (that is a {@link #connections} arm already).
     */
    public static boolean attachesTo(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos at = pos.relative(direction);
        BlockState state = level.getBlockState(at);
        if(state.isAir() || state.getBlock() instanceof TowerBeamBlock)
            return false;
        if(direction.getAxis() == Direction.Axis.Y) {
            if(state.is(RadiocraftTags.Blocks.ANTENNA_BLOCKS) || state.is(RadiocraftTags.Blocks.ANTENNA_WIRE_HOLDERS))
                return true;
            VoxelShape face = state.getShape(level, at).getFaceShape(direction.getOpposite());
            if(!face.isEmpty() && Shapes.joinIsNotEmpty(face, CENTRE_COLUMN, BooleanOp.AND))
                return true;
        }
        return !Block.isExceptionForConnection(state) && state.isFaceSturdy(level, at, direction.getOpposite(), SupportType.CENTER);
    }

    /** The core and an arm out of each face it reaches through; the diagonal braces are left to walk through. */
    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        VoxelShape shape = core;
        int mask = arms(level, pos);
        for(Direction direction : Direction.values())
            if((mask & bit(direction)) != 0)
                shape = Shapes.or(shape, faceArms[direction.ordinal()]);
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
