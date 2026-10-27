package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.radio.voice.SpeakerKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * A speaker that sits next to a radio (on any side, on top included) and plays its audio better than the radio's
 * own: see {@link SpeakerKind}. The desk speakers (one per wood) and the large speaker are {@link SpeakerKind#STANDARD},
 * the premium desk speakers {@link SpeakerKind#PREMIUM}. Its front faces the player who placed it.
 */
public class SpeakerBlock extends Block {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	private final SpeakerKind kind;
	private final Map<Direction, VoxelShape> shapes;

	/** @param box The model's bounds facing north, in pixels: {minX, minY, minZ, maxX, maxY, maxZ}. */
	@SuppressWarnings("this-escape")
	public SpeakerBlock(Properties properties, SpeakerKind kind, double... box) {
		super(properties);
		this.kind = kind;
		this.shapes = shapes(box);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	public SpeakerKind getKind() {
		return kind;
	}

	/** One box facing north, turned for each facing. */
	static Map<Direction, VoxelShape> shapes(double[] b) {
		Map<Direction, VoxelShape> out = new EnumMap<>(Direction.class);
		out.put(Direction.NORTH, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
		out.put(Direction.EAST, Block.box(16.0D - b[5], b[1], b[0], 16.0D - b[2], b[4], b[3]));
		out.put(Direction.SOUTH, Block.box(16.0D - b[3], b[1], 16.0D - b[5], 16.0D - b[0], b[4], 16.0D - b[2]));
		out.put(Direction.WEST, Block.box(b[2], b[1], 16.0D - b[3], b[5], b[4], 16.0D - b[0]));
		return out;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return shapes.get(state.getValue(FACING));
	}

	@Override
	public void appendHoverText(@NotNull net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.@NotNull TooltipContext context,
								@NotNull java.util.List<net.minecraft.network.chat.Component> tooltip, @NotNull net.minecraft.world.item.TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(net.minecraft.network.chat.Component.translatable(kind == SpeakerKind.PREMIUM ? "tooltip.radiocraft.premium_speaker" : "tooltip.radiocraft.speaker").withStyle(net.minecraft.ChatFormatting.GRAY));
	}
}
