package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.IonosphericProcessorBlockEntity;
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

/** The Ionospheric Processor's terminal: timed bulletins and blackout alerts on or off, and SEND NOW. No slots. */
public class IonosphericProcessorMenu extends AbstractContainerMenu implements StationMenu {

	public final IonosphericProcessorBlockEntity blockEntity;

	public IonosphericProcessorMenu(int id, IonosphericProcessorBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.IONOSPHERIC_PROCESSOR.get(), id);
		this.blockEntity = blockEntity;
	}

	public IonosphericProcessorMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, IonosphericProcessorBlockEntity.class));
	}

	@Override
	public BlockPos getBlockPos() {
		return blockEntity.getBlockPos();
	}

	@Override
	public void handleAction(ServerPlayer player, String action, CompoundTag data) {
		switch(action) {
			case "bulletins" -> blockEntity.setTimedBulletins(!blockEntity.hasTimedBulletins());
			case "alerts" -> blockEntity.setBlackoutAlerts(!blockEntity.hasBlackoutAlerts());
			case "send" -> {
				if(!blockEntity.sendNow())
					player.displayClientMessage(Component.translatable(blockEntity.isOnline()
							? "screen.radiocraft.ionospheric_processor.nowhere" : "screen.radiocraft.ionospheric_processor.no_ionosonde"), true);
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
