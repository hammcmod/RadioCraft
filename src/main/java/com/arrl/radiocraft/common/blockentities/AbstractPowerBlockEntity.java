package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.blocks.AbstractPowerNetworkBlock;
import com.arrl.radiocraft.common.capabilities.BasicEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public abstract class AbstractPowerBlockEntity extends BlockEntity implements MenuProvider {

	protected final BasicEnergyStorage energyStorage;
	protected final LazyOptional<IEnergyStorage> energy;

	public AbstractPowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int maxTransfer) {
		super(type, pos, state);
		energyStorage = new BasicEnergyStorage(capacity, maxTransfer);
		energy = LazyOptional.of(() -> energyStorage);
	}

	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		return cap == ForgeCapabilities.ENERGY ? energy.cast() : super.getCapability(cap, side);
	}

	public BasicEnergyStorage getEnergy() {
		return energyStorage;
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		energy.invalidate();
	}

	@Override
	protected void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		nbt.put("energy", energyStorage.serializeNBT());
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		energyStorage.deserializeNBT(nbt.get("energy"));
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(!level.isClientSide) {
			BlockState state = level.getBlockState(getBlockPos()); // Create/add to networks when loaded
			if(state.getBlock() instanceof AbstractPowerNetworkBlock block)
				block.onPlace(state, level, getBlockPos(), Blocks.AIR.defaultBlockState(), false);
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
	}

	/**
	 * Attempt to consume power from self, return false if there isn't enough
	 */
	public boolean tryConsumePower(int amount, boolean simulate) {
		return energyStorage.extractEnergy(amount, simulate) == amount;
	}

}
