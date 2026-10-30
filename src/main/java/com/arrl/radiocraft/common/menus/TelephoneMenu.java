package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.TelephoneBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** The telephone's screen; no slots. */
public class TelephoneMenu extends AbstractContainerMenu implements StationMenu {

	public final TelephoneBlockEntity blockEntity;

	public TelephoneMenu(int id, TelephoneBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.TELEPHONE.get(), id);
		this.blockEntity = blockEntity;
	}

	public TelephoneMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, TelephoneBlockEntity.class));
	}

	@Override
	public BlockPos getBlockPos() {
		return blockEntity.getBlockPos();
	}

	@Override
	public void handleAction(ServerPlayer player, String action, CompoundTag data) {
		switch(action) {
			case "dial" -> blockEntity.dial(data.getString("number"));
			case "answer" -> blockEntity.answer();
			case "hangup" -> blockEntity.hangUp();
			case "extension" -> {
				if(!blockEntity.setExtension(data.getInt("extension")))
					player.displayClientMessage(Component.translatable("screen.radiocraft.telephone.extension_taken"), true);
			}
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
