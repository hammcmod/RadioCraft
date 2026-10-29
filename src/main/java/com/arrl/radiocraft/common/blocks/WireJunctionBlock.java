package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.WireJunctionBlockEntity;
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
 * A junction box for power wire: wire of any colour runs into it from any side and joins
 * ({@link com.arrl.radiocraft.common.be_networks.network_objects.WireJunctionNetworkObject}). Colours otherwise keep
 * apart (plain is a colour of its own), so this is how two runs of different colours become one circuit. Not coax.
 */
public class WireJunctionBlock extends AbstractNetworkBlock {

	/** A metal box half a block high, the full width, so wire meets its sides. */
	public static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

	public WireJunctionBlock(Properties properties) {
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
		return new WireJunctionBlockEntity(pos, state);
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("tooltip.radiocraft.wire_junction"));
	}
}
