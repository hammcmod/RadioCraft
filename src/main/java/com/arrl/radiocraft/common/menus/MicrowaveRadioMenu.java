package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity;
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

/** The microwave radio's screen. Its state reaches the client with the block entity's update tag; no slots. */
public class MicrowaveRadioMenu extends AbstractContainerMenu implements StationMenu {

	public final MicrowaveRadioBlockEntity blockEntity;

	public MicrowaveRadioMenu(int id, MicrowaveRadioBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.MICROWAVE_RADIO.get(), id);
		this.blockEntity = blockEntity;
	}

	public MicrowaveRadioMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, MicrowaveRadioBlockEntity.class));
	}

	@Override
	public BlockPos getBlockPos() {
		return blockEntity.getBlockPos();
	}

	@Override
	public void handleAction(ServerPlayer player, String action, CompoundTag data) {
		switch(action) {
			case "power" -> blockEntity.togglePower();
			case "identity" -> {
				if(!blockEntity.setIdentity(data.getString("callsign"), data.getInt("id")))
					player.displayClientMessage(Component.translatable("screen.radiocraft.microwave.identity_taken"), true);
			}
			case "tune" -> blockEntity.tune(Math.max(-200, Math.min(200, data.getInt("steps"))));
			case "frequency" -> blockEntity.setFrequency(data.getInt("mhz"));
			case "broadcast" -> blockEntity.broadcast();
			case "search" -> blockEntity.search();
			case "confirm" -> blockEntity.confirm();
			case "reject" -> blockEntity.reject();
			case "disconnect" -> blockEntity.disconnect();
			case "dish" -> blockEntity.turnDish(data.getInt("index"), Math.max(-90.0D, Math.min(90.0D, data.getDouble("azimuth"))),
					Math.max(-90.0D, Math.min(90.0D, data.getDouble("elevation"))));
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
