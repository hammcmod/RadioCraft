package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.DuplexerBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import com.arrl.radiocraft.common.radio.DuplexerMath;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class DuplexerMenu extends AbstractContainerMenu {

	public DuplexerBlockEntity blockEntity;
	private final ContainerLevelAccess canInteractWithCallable;
	private final ContainerData data;

	public DuplexerMenu(int id, DuplexerBlockEntity blockEntity, ContainerData data) {
		super(RadiocraftMenuTypes.DUPLEXER.get(), id);
		this.blockEntity = blockEntity;
		this.data = data;
		this.canInteractWithCallable = ContainerLevelAccess.create(Objects.requireNonNull(blockEntity.getLevel()), blockEntity.getBlockPos());
		addDataSlots(data);
	}

	public DuplexerMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, DuplexerBlockEntity.class), new SimpleContainerData(DuplexerMath.SCREWS));
	}

	/** A screw's frequency in Hz, kept in sync from the server; 0 while the duplexer has never been tuned. */
	public int getScrewHz(int index) {
		int khz = data.get(index);
		return khz < 0 ? 0 : (DuplexerBlockEntity.BAND_BOTTOM_KHZ + khz) * 1000;
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return stillValid(canInteractWithCallable, player, RadiocraftBlocks.DUPLEXER.get());
	}

	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
		return ItemStack.EMPTY;
	}
}
