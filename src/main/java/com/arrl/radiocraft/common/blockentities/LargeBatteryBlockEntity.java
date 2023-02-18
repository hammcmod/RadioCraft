package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.capabilities.BasicEnergyStorage;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.power.IPowerNetworkItem;
import com.arrl.radiocraft.common.power.PowerNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class LargeBatteryBlockEntity extends BlockEntity implements IPowerNetworkItem {

	private final BasicEnergyStorage energyStorage = new BasicEnergyStorage(5000, 100); // 3000 capacity, 100 max transfer
	private final LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> energyStorage);
	private Map<Direction, PowerNetwork> networks = new HashMap<>();

	public LargeBatteryBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.LARGE_BATTERY.get(), pos, state);
	}

	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		return cap == ForgeCapabilities.ENERGY ? energy.cast() : super.getCapability(cap, side);
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
	public Map<Direction, PowerNetwork> getNetworks() {
		return networks;
	}

	@Override
	public void setNetworks(Map<Direction, PowerNetwork> networks) {
		this.networks = networks;
	}

}
