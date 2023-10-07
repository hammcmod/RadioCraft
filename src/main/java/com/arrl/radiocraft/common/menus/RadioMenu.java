package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.RadioBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RadioMenu<T extends RadioBlockEntity> extends AbstractContainerMenu {

	public T blockEntity;
	private final Block validBlock;
	private final ContainerLevelAccess canInteractWithCallable;
	private final ContainerData data;

	public RadioMenu(MenuType<?> type, final int id, final T blockEntity, ContainerData data, Block validBlock) {
		super(type, id);
		this.blockEntity = blockEntity;
		this.canInteractWithCallable = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
		this.validBlock = validBlock;
		this.data = data;
		addDataSlots(this.data);
	}

	protected T getBlockEntity(Inventory playerInventory, FriendlyByteBuf data, Class<T> clazz) {
		BlockEntity be = playerInventory.player.level.getBlockEntity(data.readBlockPos());

		if(be != null && be.getClass() != clazz)
			throw new IllegalStateException("BlockEntity at " + data.readBlockPos() + " is not the correct type");

		return (T)be;
	}

	public int getFrequency() {
		return data.get(0);
	}

	public void setFrequency(int value) {
		data.set(0, value);
	}

	public int getWavelength() {
		return data.get(1);
	}

	public void setWavelength(int value) {
		data.set(1, value);
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(canInteractWithCallable, player, validBlock);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return null;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		if(blockEntity != null) {
			blockEntity.setPTTDown(false);
		}
	}

}