package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.WefaxReceiverBlockEntity;
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

/** The WEFAX receiver's controls: WX REPT, SOLR WX and AUTO. No slots (paper goes in by using it on the machine). */
public class WefaxReceiverMenu extends AbstractContainerMenu implements StationMenu {

	public final WefaxReceiverBlockEntity blockEntity;

	public WefaxReceiverMenu(int id, WefaxReceiverBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.WEFAX_RECEIVER.get(), id);
		this.blockEntity = blockEntity;
	}

	public WefaxReceiverMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, WefaxReceiverBlockEntity.class));
	}

	@Override
	public BlockPos getBlockPos() {
		return blockEntity.getBlockPos();
	}

	@Override
	public void handleAction(ServerPlayer player, String action, CompoundTag data) {
		switch(action) {
			case "weather", "solar" -> {
				String why = blockEntity.cannotPrint(action.equals("solar"));
				if(why != null)
					player.displayClientMessage(Component.translatable(why), true);
				else if(action.equals("weather"))
					blockEntity.printWeather();
				else
					blockEntity.printSolar(player);
			}
			case "auto" -> blockEntity.setAuto(!blockEntity.isAuto());
			case "take" -> blockEntity.takeSheet(player);
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
