package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.PabxBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** The phone PABX's screen: its master number, first extension and name. No slots. */
public class PabxMenu extends AbstractContainerMenu implements StationMenu {

	public final PabxBlockEntity blockEntity;

	public PabxMenu(int id, PabxBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.PABX.get(), id);
		this.blockEntity = blockEntity;
	}

	public PabxMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, PabxBlockEntity.class));
	}

	@Override
	public BlockPos getBlockPos() {
		return blockEntity.getBlockPos();
	}

	@Override
	public void handleAction(ServerPlayer player, String action, CompoundTag data) {
		switch(action) {
			case "save" -> blockEntity.save(data.getString("master"), data.getString("first"), data.getString("name"));
			default -> {}
		}
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return !blockEntity.isRemoved() && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64.0D;
	}

	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
		return ItemStack.EMPTY;
	}
}
