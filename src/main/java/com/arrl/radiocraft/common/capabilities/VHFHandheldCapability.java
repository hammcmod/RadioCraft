package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.UnknownNullability;

import java.util.WeakHashMap;

public class VHFHandheldCapability implements IVHFHandheldCapability {

	private ItemStack heldItem = ItemStack.EMPTY;
	private boolean isPowered = false;
	private int frequencyKiloHertz = 0;
	private boolean isPTTDown = false;
	//TODO temporary hack for testing, must be changed to use data attachment API for storing state, and produce a new capability on each request
	private static final WeakHashMap<ItemStack, VHFHandheldCapability> capabilities = new WeakHashMap<>();

	public VHFHandheldCapability(ItemStack item) {
		heldItem = item;
	}

	public static IVHFHandheldCapability getCapability(ItemStack itemStack) {
		return capabilities.computeIfAbsent(itemStack, VHFHandheldCapability::new);
	}

	@Override
	public ItemStack getItem() {
		return heldItem;
	}

	@Override
	public void setItem(ItemStack item) {
		heldItem = item;
	}

	@Override
	public int getFrequencyKiloHertz() {
		return frequencyKiloHertz;
	}

	@Override
	public void setFrequencyKiloHertz(int frequencyKiloHertz) {
		this.frequencyKiloHertz = frequencyKiloHertz;
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

	//TODO obsolete as of the dataAttachment system, NBT is no longer worked with directly
	@Override
	public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
		CompoundTag nbt = new CompoundTag();
		nbt.put("inventory", heldItem.save(provider));
		nbt.putBoolean("isPowered", isPowered);
		nbt.putInt("frequency", frequencyKiloHertz);
		nbt.putBoolean("isPTTDown", isPTTDown);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
		heldItem = ItemStack.parseOptional(provider, nbt.getCompound("inventory"));
		isPowered = nbt.getBoolean("isPowered");
		frequencyKiloHertz = nbt.getInt("frequency");
		isPTTDown = nbt.getBoolean("isPTTDown");
	}
}
