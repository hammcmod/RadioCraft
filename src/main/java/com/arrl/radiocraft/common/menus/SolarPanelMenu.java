package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.SolarPanelBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class SolarPanelMenu extends AbstractContainerMenu {

	public final SolarPanelBlockEntity blockEntity;
	private final ContainerLevelAccess canInteractWithCallable;
	private final ContainerData data;

	public SolarPanelMenu(final int id, final SolarPanelBlockEntity blockEntity, ContainerData data) {
		super(RadiocraftMenuTypes.SOLAR_PANEL.get(), id);
		this.blockEntity = blockEntity;
		this.canInteractWithCallable = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
		this.data = data;
		addDataSlots(this.data);
	}

	public SolarPanelMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, SolarPanelBlockEntity.class), new SimpleContainerData(1));
	}

	public int getPowerTick() {
		return data.get(0);
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(canInteractWithCallable, player, RadiocraftBlocks.SOLAR_PANEL.get());
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return null;
	}

}