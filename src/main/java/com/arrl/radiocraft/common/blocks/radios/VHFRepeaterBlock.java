package com.arrl.radiocraft.common.blocks.radios;

import com.arrl.radiocraft.common.radio.crystal.CrystalQuality;
import com.arrl.radiocraft.common.blockentities.VHFRepeaterBlockEntity;
import com.arrl.radiocraft.common.radio.Callsigns;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** The VHF repeater: a radio block (ticking, comparator S-meter, facing) whose back, top or bottom takes a duplexer. */
public class VHFRepeaterBlock extends RadioBlock {

	public static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);

	public VHFRepeaterBlock(Properties properties) {
		super(properties, CrystalQuality.PURE); // Repeaters are only built with pure crystals.
	}

	@Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return hanging(state, SHAPE);
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return null;
	}

	/** A new repeater gets a callsign: made up in the X#XX form, or with repeater_callsign_mode OWN its placer's. */
	@Override
	public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if(level.isClientSide || !(level.getBlockEntity(pos) instanceof VHFRepeaterBlockEntity repeater) || !repeater.getCallsign().isEmpty())
			return;
		String callsign = Callsigns.repeaterMode() == Callsigns.RepeaterMode.OWN
				? (placer instanceof ServerPlayer player && player.getServer() != null ? Callsigns.playerCallsign(player.getServer(), player.getUUID()) : null)
				: Callsigns.generateRepeater(level, pos, level.getRandom());
		if(callsign != null) {
			CompoundTag tag = new CompoundTag();
			tag.putString("callsign", callsign);
			repeater.applySettings(tag);
		}
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new VHFRepeaterBlockEntity(pos, state);
	}
}
