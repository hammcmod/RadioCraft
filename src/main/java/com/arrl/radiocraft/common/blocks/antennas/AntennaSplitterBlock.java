package com.arrl.radiocraft.common.blocks.antennas;

import com.arrl.radiocraft.common.blockentities.AntennaSplitterBlockEntity;
import com.arrl.radiocraft.common.blocks.AbstractNetworkBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The receive-only antenna splitter (maintainer, 2026-10-27): coax from one antenna in, coax to several receivers out,
 * on any sides ({@link com.arrl.radiocraft.common.be_networks.network_objects.AntennaSplitterNetworkObject}). Every
 * radio behind it hears the antenna, each a share of the signal; none can transmit through it. A placeholder box until
 * it has a model (doc/ModelDesignBounty.md).
 */
public class AntennaSplitterBlock extends AbstractNetworkBlock {

	/** A small metal box, the full width so coax meets its sides. */
	public static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D);

	public AntennaSplitterBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return null;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new AntennaSplitterBlockEntity(pos, state);
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("tooltip.radiocraft.antenna_splitter"));
	}
}
