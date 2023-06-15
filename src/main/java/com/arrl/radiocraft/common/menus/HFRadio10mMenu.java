package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.HFRadio10mBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntity;

public class HFRadio10mMenu extends AbstractContainerMenu {

	public final HFRadio10mBlockEntity blockEntity;
	private final ContainerLevelAccess canInteractWithCallable;
	private final ContainerData data;

	public HFRadio10mMenu(final int id, final HFRadio10mBlockEntity blockEntity, ContainerData data) {
		super(RadiocraftMenuTypes.HF_RADIO_10M.get(), id);
		this.blockEntity = blockEntity;
		this.canInteractWithCallable = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
		this.data = data;
		addDataSlots(this.data);
	}

	public HFRadio10mMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, getBlockEntity(playerInventory, data), new SimpleContainerData(2));
	}

	private static HFRadio10mBlockEntity getBlockEntity(final Inventory playerInventory, final FriendlyByteBuf data) {
		final BlockEntity blockEntity = playerInventory.player.level.getBlockEntity(data.readBlockPos());

		if(blockEntity instanceof HFRadio10mBlockEntity) {
			return (HFRadio10mBlockEntity)blockEntity;
		}
		throw new IllegalStateException("BlockEntity at " + data.readBlockPos() + " is not correct");
	}

	public boolean isPowered() {
		return blockEntity.isPowered();
	}

	public boolean isTransmitting() {
		return blockEntity.isTransmitting();
	}

	public boolean isReceiving() {
		return blockEntity.isReceiving();
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(canInteractWithCallable, player, RadiocraftBlocks.HF_RADIO_10M.get());
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return null;
	}

}