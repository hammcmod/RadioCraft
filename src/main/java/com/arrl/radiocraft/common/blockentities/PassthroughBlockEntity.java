package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Remembers which block a passthrough replaced, so the renderer can keep drawing it and breaking gives it back. */
public class PassthroughBlockEntity extends BlockEntity {

	private static final String MIMIC_TAG = "mimic";
	@Nullable
	private BlockState mimic = Blocks.STONE.defaultBlockState();

	public PassthroughBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.PASSTHROUGH.get(), pos, state);
	}

	@Nullable
	public BlockState getMimic() {
		return mimic;
	}

	public void setMimic(@Nullable BlockState state) {
		this.mimic = state;
		setChanged();
		if(level != null && !level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.saveAdditional(tag, registries);
		if(mimic != null)
			tag.put(MIMIC_TAG, NbtUtils.writeBlockState(mimic));
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		if(tag.contains(MIMIC_TAG))
			mimic = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound(MIMIC_TAG));
	}

	@Override
	public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Nullable
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
