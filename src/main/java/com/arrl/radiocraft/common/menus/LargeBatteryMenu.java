package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.LargeBatteryBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import com.arrl.radiocraft.common.menus.slots.IntSplitDataSlot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class LargeBatteryMenu extends AbstractContainerMenu {

	public final LargeBatteryBlockEntity blockEntity;
	private final ContainerLevelAccess canInteractWithCallable;
	private final IntSplitDataSlot dataEnergy;
	private final IntSplitDataSlot dataMaxEnergy;

    @SuppressWarnings("this-escape")
	public LargeBatteryMenu(final int id, final LargeBatteryBlockEntity blockEntity, IntSplitDataSlot dataEnergy, IntSplitDataSlot dataMaxEnergy) {
		super(RadiocraftMenuTypes.LARGE_BATTERY.get(), id);
		this.blockEntity = blockEntity;
		this.canInteractWithCallable = ContainerLevelAccess.create(Objects.requireNonNull(blockEntity.getLevel()), blockEntity.getBlockPos());
		this.dataEnergy = dataEnergy;
		this.dataMaxEnergy = dataMaxEnergy;

		// Because the NetworkObject storing the BE's power doesn't exist clientside, we use dummy slots on the client
		// while the BE passes in appropriate DataSlots on the server.
		addDataSlots(dataEnergy);
		addDataSlots(dataMaxEnergy);
	}

	public LargeBatteryMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, LargeBatteryBlockEntity.class), new IntSplitDataSlot(), new IntSplitDataSlot());
	}

	public int getCurrentPower() {
		return dataEnergy.get(2);
	}

	public int getMaxPower() {
		return dataMaxEnergy.get(2);
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return stillValid(canInteractWithCallable, player, RadiocraftBlocks.LARGE_BATTERY.get());
	}

	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
		return null;
	}

}