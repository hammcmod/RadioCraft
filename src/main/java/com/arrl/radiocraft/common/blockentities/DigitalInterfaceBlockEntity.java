package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.DigitalInterfaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DigitalInterfaceBlockEntity extends PowerBlockEntity {

	// Tab indices: 0 = RTTY, 1 = ARPS, 2 = MSG, 3 = FILES
	private int selectedTab = 0;

	private final ContainerData dataSlots = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> selectedTab;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
			if (index == 0) {
				selectedTab = value;
			}
		}

		@Override
		public int getCount() {
			return 1;
		}
	};

	public DigitalInterfaceBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.DIGITAL_INTERFACE.get(), pos, state);
	}

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("container.radiocraft.digital_interface");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
		return new DigitalInterfaceMenu(id, this);
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return null; // Digital Interface doesn't need a network object for now
	}

	public ContainerData getDataSlots() {
		return dataSlots;
	}

	public int getSelectedTab() {
		return selectedTab;
	}

	public void setSelectedTab(int tab) {
		this.selectedTab = tab;
		setChanged();
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, @NotNull net.minecraft.core.HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt("SelectedTab", selectedTab);
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, @NotNull net.minecraft.core.HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		selectedTab = tag.getInt("SelectedTab");
	}
}
