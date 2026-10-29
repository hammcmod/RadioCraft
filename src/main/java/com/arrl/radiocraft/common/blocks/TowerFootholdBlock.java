package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.RadiocraftServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * A rung on a tower: climbed like a ladder and stood on from above. The old one (found on towers in structures) may
 * give way each time a player climbs onto it or steps on it, {@code tower_foothold_break_chance} of the time, with a
 * metal clang: the rung bends down ({@link #BROKEN}), and a broken foothold can no longer be climbed or stood on. The
 * footholds players craft never break.
 * <p>
 * Like a ladder it needs a solid wall behind it, or a tower beam: on a beam ({@link #MOUNT}) it is drawn further back,
 * against the steel, since a beam is thinner than its block.
 */
public class TowerFootholdBlock extends LadderBlock {

    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");
    /** What the foothold hangs on: a wall, or a beam of one of the three thicknesses (how far back it sits). */
    public static final EnumProperty<Mount> MOUNT = EnumProperty.create("mount", Mount.class);

    /** See {@link #MOUNT}. {@code reach}: pixels back from the block's edge to the steel (8 minus half the beam's width). */
    public enum Mount implements StringRepresentable {
        WALL(0), THICK_BEAM(4), BEAM(5), THIN_BEAM(6);

        public final int reach;

        Mount(int reach) {
            this.reach = reach;
        }

        /** What a foothold hanging on {@code wall} is mounted on. */
        public static Mount of(BlockState wall) {
            if(!(wall.getBlock() instanceof TowerBeamBlock beam))
                return WALL;
            int reach = 8 - beam.getWidth() / 2;
            for(Mount mount : values())
                if(mount.reach == reach)
                    return mount;
            return WALL;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final boolean fragile;

    @SuppressWarnings("this-escape")
    public TowerFootholdBlock(Properties properties, boolean fragile) {
        super(properties);
        this.fragile = fragile;
        registerDefaultState(defaultBlockState().setValue(BROKEN, false).setValue(MOUNT, Mount.WALL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BROKEN, MOUNT);
    }

    /** The block the foothold in {@code state} at {@code pos} hangs on. */
    private static BlockPos wallPos(BlockState state, BlockPos pos) {
        return pos.relative(state.getValue(FACING).getOpposite());
    }

    /** The foothold directly under {@code pos} facing the same way, which can carry this one: footholds stack. */
    @Nullable
    private static BlockState footholdBelow(LevelReader level, BlockPos pos, Direction facing) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof TowerFootholdBlock && below.getValue(FACING) == facing ? below : null;
    }

    /**
     * Hangs on a sturdy wall, as a ladder does, or on a tower beam, or stands on a foothold below it facing the same
     * way (footholds stack, with nothing behind them). A stack stands as long as its bottom one does.
     */
    @Override
    protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return level.getBlockState(wallPos(state, pos)).getBlock() instanceof TowerBeamBlock || super.canSurvive(state, level, pos)
                || footholdBelow(level, pos, state.getValue(FACING)) != null;
    }

    /**
     * Where the foothold in {@code state} at {@code pos} sits: against the beam or wall behind it, or, with nothing to
     * bear on behind it, in line with the foothold it stands on, so a stack up a beam stays against the steel.
     */
    private static Mount mountFor(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos wall = wallPos(state, pos);
        BlockState behind = level.getBlockState(wall);
        if(behind.getBlock() instanceof TowerBeamBlock || behind.isFaceSturdy(level, wall, state.getValue(FACING)))
            return Mount.of(behind);
        BlockState below = footholdBelow(level, pos, state.getValue(FACING));
        return below != null ? below.getValue(MOUNT) : Mount.WALL;
    }

    /** On top of another foothold it carries the stack on, facing its way; otherwise placed as a ladder. */
    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState below = level.getBlockState(pos.below());
        BlockState state = null;
        if(below.getBlock() instanceof TowerFootholdBlock && context.getClickedFace() == Direction.UP)
            state = defaultBlockState().setValue(FACING, below.getValue(FACING));
        if(state == null)
            state = super.getStateForPlacement(context);
        if(state == null && below.getBlock() instanceof TowerFootholdBlock)
            state = defaultBlockState().setValue(FACING, below.getValue(FACING)); // Clicked beside it, nothing to hang on.
        if(state == null || !state.canSurvive(level, pos))
            return null;
        boolean water = level.getFluidState(pos).getType() == net.minecraft.world.level.material.Fluids.WATER;
        return state.setValue(WATERLOGGED, water).setValue(MOUNT, mountFor(level, pos, state));
    }

    @Override
    protected @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState,
                                              @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        if(updated.getBlock() != this)
            return updated;
        if(direction == Direction.DOWN && !updated.canSurvive(level, pos))
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(); // The foothold it stood on is gone.
        if(direction == state.getValue(FACING).getOpposite() || direction == Direction.DOWN)
            updated = updated.setValue(MOUNT, mountFor(level, pos, updated)); // The beam behind it changed, or the one below moved.
        return updated;
    }

    /** The ladder's slab, moved back against the beam it hangs on. */
    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        VoxelShape shape = super.getShape(state, level, pos, context);
        int reach = state.getValue(MOUNT).reach;
        if(reach == 0)
            return shape;
        Direction back = state.getValue(FACING).getOpposite();
        return shape.move(back.getStepX() * reach / 16.0D, 0.0D, back.getStepZ() * reach / 16.0D);
    }

    public boolean isFragile() {
        return fragile;
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return !state.getValue(BROKEN);
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return state.getValue(BROKEN) ? Shapes.empty() : super.getCollisionShape(state, level, pos, context);
    }

    /** Climbing: the player's feet have just come into this block. */
    @Override
    protected void entityInside(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        if(entity.blockPosition().equals(pos) && !BlockPos.containing(entity.xo, entity.yo, entity.zo).equals(pos))
            playerArrived(level, pos, state, entity);
    }

    /** Standing: the player has just stepped onto this block from somewhere else. */
    @Override
    public void stepOn(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Entity entity) {
        if(!BlockPos.containing(entity.xo, entity.yo - 0.2D, entity.zo).equals(pos))
            playerArrived(level, pos, state, entity);
        super.stepOn(level, pos, state, entity);
    }

    /** A player climbed onto or stepped on the foothold: an old one may give way. Server side only. */
    public void playerArrived(Level level, BlockPos pos, BlockState state, Entity entity) {
        if(level.isClientSide || !fragile || state.getValue(BROKEN) || !(entity instanceof Player player) || player.isSpectator())
            return;
        if(level.random.nextDouble() < RadiocraftServerConfig.TOWER_FOOTHOLD_BREAK_CHANCE.get())
            breakRung(level, pos, state);
    }

    /** Bends the rung down with a clang (an anvil, sped up). */
    public static void breakRung(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(BROKEN, true), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6F, 1.8F);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(fragile ? "tooltip.radiocraft.old_tower_foothold" : "tooltip.radiocraft.tower_foothold"));
    }
}
