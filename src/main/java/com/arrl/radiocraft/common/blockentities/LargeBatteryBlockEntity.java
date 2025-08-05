
package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.common.data.PowerNetworkSavedData;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.LargeBatteryMenu;
import com.arrl.radiocraft.common.menus.slots.IntRefSplitDataSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LargeBatteryBlockEntity extends BlockEntity implements MenuProvider, IEnergyStorage {

	private int transferredEnergyLastTick = 0;

	private final EnergyStorage energyStorage = new EnergyStorage(
			CommonConfig.LARGE_BATTERY_CAPACITY.get(),
			CommonConfig.LARGE_BATTERY_OUTPUT.get(),
			CommonConfig.LARGE_BATTERY_OUTPUT.get()
	);

	public LargeBatteryBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.LARGE_BATTERY.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, LargeBatteryBlockEntity be) {
		if (level.isClientSide) return;

		// Battery distributes stored energy using unified method
		be.distributeEnergyToNetwork((ServerLevel) level);
	}

	private void distributeEnergyToNetwork(ServerLevel level) {
		transferredEnergyLastTick = 0;
		if (energyStorage.getEnergyStored() == 0) return;

		PowerNetworkSavedData networkData = PowerNetworkSavedData.get(level);
		int transferred = networkData.distributeStoragePower(
				level,
				worldPosition,
				energyStorage.getEnergyStored(),
				CommonConfig.LARGE_BATTERY_OUTPUT.get()
		);

		transferredEnergyLastTick = transferred;
		energyStorage.extractEnergy(transferred, false);
	}

	public int getTransferredEnergyLastTick() {
		return transferredEnergyLastTick;
	}

	public IEnergyStorage getEnergyStorage() {
		return energyStorage;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level instanceof ServerLevel serverLevel) {
			PowerNetworkSavedData networkData = PowerNetworkSavedData.get(serverLevel);
			var network = networkData.getNetwork(worldPosition);
			if (network == null) {
				network = networkData.createNetwork();
				networkData.addToNetwork(worldPosition, network);
			}
		}
	}

	@Override
	public void setRemoved() {
		if (level instanceof ServerLevel serverLevel) {
			PowerNetworkSavedData networkData = PowerNetworkSavedData.get(serverLevel);
			networkData.removeFromNetwork(worldPosition);
		}
		super.setRemoved();
	}

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("container.large_battery");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
		return new LargeBatteryMenu(id, this,
				new IntRefSplitDataSlot(
						value -> {}, // Client can never set this anyway
						() -> energyStorage.getEnergyStored()
				),
				new IntRefSplitDataSlot(
						value -> {}, // Client can never set this anyway
						() -> energyStorage.getMaxEnergyStored()
				)
		);
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("energy", energyStorage.serializeNBT(registries));
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains("energy")) {
			energyStorage.deserializeNBT(registries, tag.get("energy"));
		}
	}

	@Override
	public int receiveEnergy(int i, boolean b) {
		int transferred = energyStorage.receiveEnergy(i, b);
		transferredEnergyLastTick += transferred;
		return transferred;
	}

	@Override
	public int extractEnergy(int i, boolean b) {
		int transferred = energyStorage.extractEnergy(i, b);
		transferredEnergyLastTick -= transferred;
		return transferred;
	}

	@Override
	public int getEnergyStored() {
		return energyStorage.getEnergyStored();
	}

	@Override
	public int getMaxEnergyStored() {
		return energyStorage.getMaxEnergyStored();
	}

	@Override
	public boolean canExtract() {
		return energyStorage.canExtract();
	}

	@Override
	public boolean canReceive() {
		return energyStorage.canReceive();
	}
}