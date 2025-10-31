package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.DigitalInterfaceBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DigitalInterfaceMenu extends AbstractContainerMenu {

	public DigitalInterfaceBlockEntity blockEntity;
	private final ContainerLevelAccess canInteractWithCallable;
	private final ContainerData data;

	public DigitalInterfaceMenu(int id, DigitalInterfaceBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.DIGITAL_INTERFACE.get(), id);
		this.blockEntity = blockEntity;
		this.canInteractWithCallable = ContainerLevelAccess.create(Objects.requireNonNull(blockEntity.getLevel()), blockEntity.getBlockPos());
		this.data = blockEntity.getDataSlots();
		addDataSlots(data);
	}

	public DigitalInterfaceMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, DigitalInterfaceBlockEntity.class));
	}

	public int getSelectedTab() {
		return data.get(0);
	}

	public void setSelectedTab(int tab) {
		blockEntity.setSelectedTab(tab);
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return stillValid(canInteractWithCallable, player, RadiocraftBlocks.DIGITAL_INTERFACE.get());
	}

	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
		return ItemStack.EMPTY;
	}
}
