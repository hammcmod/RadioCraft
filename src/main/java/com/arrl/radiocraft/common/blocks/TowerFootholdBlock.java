package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.RadiocraftServerConfig;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A rung on a tower: climbed like a ladder and stood on from above. The old one (found on towers in structures) may
 * give way each time a player climbs onto it or steps on it, {@code tower_foothold_break_chance} of the time, with a
 * metal clang: the rung bends down ({@link #BROKEN}), and a broken foothold can no longer be climbed or stood on. The
 * footholds players craft never break.
 */
public class TowerFootholdBlock extends LadderBlock {

    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");

    private final boolean fragile;

    @SuppressWarnings("this-escape")
    public TowerFootholdBlock(Properties properties, boolean fragile) {
        super(properties);
        this.fragile = fragile;
        registerDefaultState(defaultBlockState().setValue(BROKEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BROKEN);
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
