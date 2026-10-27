package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.WiredSpeakerBlockEntity;
import com.arrl.radiocraft.common.radio.voice.SpeakerKind;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A speaker run off a radio on a wire (the power wire, which doubles as the speaker line): the wall speaker and the
 * loudspeaker. Any number of them can share one radio's wire run, each playing the radio's audio where it stands
 * (see {@link SpeakerKind#WALL} and {@link SpeakerKind#LOUDSPEAKER}). The wall speaker goes flat against the wall it
 * is placed on; the loudspeaker's horn points the way the player looks.
 */
public class WiredSpeakerBlock extends AbstractNetworkBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final SpeakerKind kind;
    private final Map<Direction, VoxelShape> shapes;
    private final double[] box;

    @SuppressWarnings("this-escape")
    public WiredSpeakerBlock(Properties properties, SpeakerKind kind, double... box) {
        super(properties);
        this.kind = kind;
        this.box = box;
        this.shapes = SpeakerBlock.shapes(box);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public SpeakerKind getKind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        Direction facing;
        if(kind == SpeakerKind.WALL) // Flat on the wall clicked, facing out of it; on a floor, facing the player.
            facing = context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : context.getHorizontalDirection().getOpposite();
        else
            facing = context.getHorizontalDirection(); // The horn points away from the player.
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new WiredSpeakerBlock(properties, kind, box));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new WiredSpeakerBlockEntity(pos, state);
    }

    @Override
    public void appendHoverText(@NotNull net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.@NotNull TooltipContext context,
                                @NotNull java.util.List<net.minecraft.network.chat.Component> tooltip, @NotNull net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(net.minecraft.network.chat.Component.translatable(kind == SpeakerKind.WALL ? "tooltip.radiocraft.wall_speaker" : "tooltip.radiocraft.loudspeaker").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
