package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.common.be_networks.network_objects.ChargeControllerNetworkObject;
import com.arrl.radiocraft.common.blocks.power.ChargeControllerBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.menus.ChargeControllerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChargeControllerBlockEntity extends BlockEntity implements ITogglableBE, INetworkObjectProvider, MenuProvider {

	public final ItemStackHandler inventory = new ItemStackHandler(1);

	// Using a ContainerData for one value is awkward, but it changes constantly and needs to be synchronised.
	private final ContainerData fields = new ContainerData() {
		@Override
		public int get(int index) {
			if(index == 0)
				return ((ChargeControllerNetworkObject)getNetworkObject(level, worldPosition)).getLastPowerTick();
			return 0;
		}

		@Override
		public void set(int index, int value) {
			// No set behaviour, because the client is never able to change this value anyway. Send a packet instead.
		}

		@Override
		public int getCount() {
			return 1;
		}
	};

	public ChargeControllerBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.CHARGE_CONTROLLER.get(), pos, state);
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
		if(t instanceof ChargeControllerBlockEntity be) {
			if(!level.isClientSide && be.getPoweredOn()) { // Serverside only
				if(be.inventory.getStackInSlot(0).getItem() == RadiocraftItems.SMALL_BATTERY.get()) {
					ItemStack battery = be.inventory.getStackInSlot(0);

					/*

					TODO: Fix battery mechanics

					CompoundTag nbt = battery.getOrCreateTag();

					if(!nbt.contains("charge"))
						nbt.putInt("charge", 0);

					int charge = nbt.getInt("charge");
					if(charge < CommonConfig.SMALL_BATTERY_CAPACITY.get() && be.getNetworkObject(level, pos) instanceof ChargeControllerNetworkObject networkObject) {
						int toPush = Math.min(CommonConfig.CHARGE_CONTROLLER_BATTERY_CHARGE.get(), Math.min(networkObject.getStorage().getEnergyStored(), CommonConfig.SMALL_BATTERY_CAPACITY.get() - charge));
						nbt.putInt("charge", charge + toPush);
					}*/
				} // Battery charging is handled here as the ItemStack doesn't exist when the BE isn't loaded.
			}
		}
	}

	@Override
	public void toggle() {
        assert level != null;
        if(!level.isClientSide) {
			// Unlike other power BEs, the charge controller is the one in control of power as it never gets turned
			// off automatically.
			BlockState state = level.getBlockState(worldPosition);
			boolean isEnabled = state.getValue(ChargeControllerBlock.POWERED);

			if(getNetworkObject(level, worldPosition) instanceof ChargeControllerNetworkObject networkObject)
				networkObject.isEnabled = !isEnabled;

			level.setBlockAndUpdate(worldPosition, state.setValue(ChargeControllerBlock.POWERED, !isEnabled));
		}
	}

	public boolean getPoweredOn() {
		return level != null && level.getBlockState(worldPosition).getValue(ChargeControllerBlock.POWERED);
	}

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("container.charge_controller");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
		return new ChargeControllerMenu(id, playerInventory, this, fields);
	}

	@Override
	protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
		super.loadAdditional(pTag, pRegistries);
		inventory.deserializeNBT(pRegistries, pTag.getCompound("inventory"));
	}

	@Override
	protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
		super.saveAdditional(pTag, pRegistries);
		pTag.put("inventory", inventory.serializeNBT(pRegistries));
	}

	/*
	TODO: IDK what this ever did. Review and fix or remove.

	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
		return cap == ForgeCapabilities.ITEM_HANDLER ? inventoryHandler.cast() : super.getCapability(cap);
	}

	@Override
	public void setRemoved() {
		if(inventoryHandler != null)
			inventoryHandler.invalidate();
		super.setRemoved();
	}

	 */

	@Override
	public BENetworkObject createNetworkObject() {
        assert level != null;
        return new ChargeControllerNetworkObject(level, worldPosition, level.getBlockState(worldPosition).getValue(ChargeControllerBlock.POWERED));
	}

	@Override
	public void onLoad() {
		super.onLoad();
        assert level != null;
        if(!level.isClientSide())
			getNetworkObject(level, worldPosition); // This forces the network object to get initialised.
	}

}

