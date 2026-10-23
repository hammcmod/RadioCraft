package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.LargeBatteryMenu;
import com.arrl.radiocraft.common.menus.slots.IntRefSplitDataSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The large battery block. Its charge lives in its {@link BatteryNetworkObject}, which only exists in memory, so the
 * block entity writes the charge into its own tag on save and hands it back to the object when that is created
 * again after a reload. The object marks the block entity changed whenever the charge moves, so the chunk is saved.
 */
public class LargeBatteryBlockEntity extends PowerBlockEntity {

	private static final String ENERGY_TAG = "energy";
	private int savedEnergy = 0;

	public LargeBatteryBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.LARGE_BATTERY.get(), pos, state);
	}

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("container.large_battery");
	}

	/**
	 * The battery's charge as Forge Energy, for other mods' generators and cables (a Create alternator, ...): what they
	 * put in or take out is the charge the power network uses. Other mods keep hold of this object, and the network
	 * object behind it can be replaced (chunk reloads), so every call looks the current one up; with none it holds nothing.
	 */
	public final net.neoforged.neoforge.energy.IEnergyStorage energyStorage = new net.neoforged.neoforge.energy.IEnergyStorage() {
		@Override
		public int receiveEnergy(int toReceive, boolean simulate) {
			BatteryNetworkObject battery = battery();
			return battery == null ? 0 : battery.getStorage().receiveEnergy(toReceive, simulate);
		}

		@Override
		public int extractEnergy(int toExtract, boolean simulate) {
			BatteryNetworkObject battery = battery();
			return battery == null ? 0 : battery.getStorage().extractEnergy(toExtract, simulate);
		}

		@Override
		public int getEnergyStored() {
			BatteryNetworkObject battery = battery();
			return battery == null ? savedEnergy : battery.getStorage().getEnergyStored();
		}

		@Override
		public int getMaxEnergyStored() {
			BatteryNetworkObject battery = battery();
			return battery == null ? CommonConfig.LARGE_BATTERY_CAPACITY.get() : battery.getStorage().getMaxEnergyStored();
		}

		@Override
		public boolean canExtract() {
			return true;
		}

		@Override
		public boolean canReceive() {
			return true;
		}
	};

	@Nullable
	private BatteryNetworkObject battery() {
		return level != null && getNetworkObject(level, worldPosition) instanceof BatteryNetworkObject battery ? battery : null;
	}

	// This may not be right, but it makes the GUI render.
	private Supplier<Integer> getEnergyStored() {
		BatteryNetworkObject battery = battery();
		return battery == null ? null : battery.getStorage()::getEnergyStored;
	}

	private Supplier<Integer> getMaxEnergyStored() {
		BatteryNetworkObject battery = battery();
		return battery == null ? null : battery.getStorage()::getMaxEnergyStored;
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
		return new LargeBatteryMenu(id, this,
				new IntRefSplitDataSlot(
						value -> {}, // Client can never set this anyway.
						getEnergyStored()
				),
				new IntRefSplitDataSlot(
						value -> {}, // Client can never set this anyway.
						getMaxEnergyStored()
				));
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new BatteryNetworkObject(level, worldPosition,
				CommonConfig.LARGE_BATTERY_CAPACITY.get(),
				CommonConfig.LARGE_BATTERY_OUTPUT.get(),
				CommonConfig.LARGE_BATTERY_OUTPUT.get()
		);
	}

	/** A fresh object after a reload starts with the charge the block entity saved. */
	@Override
	public void onNetworkObjectCreated(Level level, BlockPos pos, BENetworkObject networkObject) {
		if(networkObject instanceof BatteryNetworkObject battery)
			battery.getStorage().setEnergy(Math.min(savedEnergy, battery.getStorage().getMaxEnergyStored()));
	}

	/**
	 * Called by the network object whenever its charge moves. Saving must not touch the level (a chunk being saved
	 * on unload would be revived by the capability lookup and never unload), so the block entity keeps its own copy.
	 */
	public void rememberEnergy(int energy) {
		if(savedEnergy != energy) {
			savedEnergy = energy;
			setChanged();
		}
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt(ENERGY_TAG, savedEnergy);
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		savedEnergy = tag.getInt(ENERGY_TAG);
	}
}
