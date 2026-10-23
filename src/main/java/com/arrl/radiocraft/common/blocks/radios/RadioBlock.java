package com.arrl.radiocraft.common.blocks.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.radio.crystal.CrystalQuality;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.arrl.radiocraft.common.blocks.power.AbstractPowerNetworkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A radio: it faces the player who placed it, its comparator output is its S-meter, and placed against the underside
 * of a block it hangs from it the right way up ({@link #CEILING}), like a rig on an under-shelf mount.
 */
public abstract class RadioBlock extends AbstractPowerNetworkBlock {

	public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** True for a radio hanging from the block above: drawn and shaped raised so its top meets that block. */
	public static final BooleanProperty CEILING = BooleanProperty.create("ceiling");
	private static final Map<VoxelShape, VoxelShape> RAISED = new ConcurrentHashMap<>();

	/** {@code floor}, the radio's shape standing on the floor, raised to meet the ceiling if {@code state} hangs. */
	protected static VoxelShape hanging(BlockState state, VoxelShape floor) {
		if(!state.getValue(CEILING) || floor.isEmpty())
			return floor;
		return RAISED.computeIfAbsent(floor, shape -> shape.move(0.0D, 1.0D - shape.max(Direction.Axis.Y), 0.0D));
	}

	private final CrystalQuality crystal;

	public RadioBlock(Properties properties) {
		this(properties, CrystalQuality.GOOD);
	}

	/** A radio built around a crystal of {@code crystal} quality (each quality is its own block). */
	public RadioBlock(Properties properties, CrystalQuality crystal) {
		super(properties);
		this.crystal = crystal;
		registerDefaultState(defaultBlockState().setValue(CEILING, false));
	}

	public CrystalQuality getCrystalQuality() {
		return crystal;
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
		if(crystal != CrystalQuality.GOOD)
			tooltipComponents.add(Component.translatable(Radiocraft.translationKey("tooltip", "crystal_" + crystal.getId())).withStyle(ChatFormatting.GRAY));
	}


	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return level.isClientSide ? RadioBlockEntity::clientTick : RadioBlockEntity::tick;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(HORIZONTAL_FACING, CEILING);
	}

	// A comparator against any radio reads its S-meter: 0 with nothing coming in, up to 15 for a full-strength signal.

	@Override
	protected boolean hasAnalogOutputSignal(@NotNull BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
		return level.getBlockEntity(pos) instanceof RadioBlockEntity radio ? radio.getMeterReading() : 0;
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite())
				.setValue(CEILING, context.getClickedFace() == Direction.DOWN);
	}

}
