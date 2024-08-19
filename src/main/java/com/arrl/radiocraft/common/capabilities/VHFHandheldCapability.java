package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.UnknownNullability;

public class VHFHandheldCapability implements IVHFHandheldCapability {

	private ItemStack heldItem = ItemStack.EMPTY;
	private boolean isPowered = false;
	private int frequency = 0;
	private boolean isPTTDown = false;

	@Override
	public ItemStack getItem() {
		return heldItem;
	}

	@Override
	public void setItem(ItemStack item) {
		heldItem = item;
	}

	@Override
	public int getFrequency() {
		return frequency;
	}

	@Override
	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	@Override
	public boolean isPowered() {
		return isPowered;
	}

	@Override
	public void setPowered(boolean value) {
		isPowered = value;
	}

	@Override
	public boolean isPTTDown() {
		return isPTTDown;
	}

	@Override
	public void setPTTDown(boolean value) {
		isPTTDown = value;
	}

	@Override
	public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
		CompoundTag nbt = new CompoundTag();
		nbt.put("inventory", heldItem.save(provider));
		nbt.putBoolean("isPowered", isPowered);
		nbt.putInt("frequency", frequency);
		nbt.putBoolean("isPTTDown", isPTTDown);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
		heldItem = ItemStack.parseOptional(provider, nbt.getCompound("inventory"));
		isPowered = nbt.getBoolean("isPowered");
		frequency = nbt.getInt("frequency");
		isPTTDown = nbt.getBoolean("isPTTDown");
	}
}
