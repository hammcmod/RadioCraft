package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.LargeBatteryMenu;
import com.arrl.radiocraft.common.menus.slots.IntRefSplitDataSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class LargeBatteryBlockEntity extends PowerBlockEntity {

	public LargeBatteryBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.LARGE_BATTERY.get(), pos, state);
	}

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("container.large_battery");
	}

	// This may not be right, but it makes the GUI render.
	private Supplier<Integer> getEnergyStored() {
		try {
			return ((BatteryNetworkObject) getNetworkObject(level, worldPosition)).getStorage()::getEnergyStored;
		} catch (NullPointerException e) {
			return null;
		}
	}

	private Supplier<Integer> getMaxEnergyStored() {
		try {
			return ((BatteryNetworkObject)getNetworkObject(level, worldPosition)).getStorage()::getMaxEnergyStored;
		} catch (NullPointerException e) {
			return null;
		}
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

}
