package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.CommercialRadioModuleBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.voice.StationProgram;
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

/**
 * Setting up a commercial radio module, for a player in creative (building a structure): its program and frequency.
 * Everyone else finds it sealed. Every change is checked against creative again on the server.
 */
public class CommercialRadioModuleMenu extends AbstractContainerMenu implements StationMenu {

	public final CommercialRadioModuleBlockEntity blockEntity;

	public CommercialRadioModuleMenu(int id, CommercialRadioModuleBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.COMMERCIAL_RADIO_MODULE.get(), id);
		this.blockEntity = blockEntity;
	}

	public CommercialRadioModuleMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, CommercialRadioModuleBlockEntity.class));
	}

	@Override
	public BlockPos getBlockPos() {
		return blockEntity.getBlockPos();
	}

	@Override
	public void handleAction(ServerPlayer player, String action, CompoundTag data) {
		if(!player.isCreative() || !action.equals("configure"))
			return;
		float hz = data.getFloat("frequency");
		Band band = Band.getBand(hz);
		if(band == null) {
			player.displayClientMessage(Component.translatable("screen.radiocraft.commercial_radio_module.no_band"), true);
			return;
		}
		blockEntity.configure(StationProgram.Kind.byName(data.getString("program")), band, hz);
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return player.isCreative() && !blockEntity.isRemoved() && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64.0D;
	}

	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
		return ItemStack.EMPTY;
	}
}
