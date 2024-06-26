package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.api.capabilities.RadiocraftCapabilities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VHFHandheldCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

	private final IVHFHandheldCapability backend = new VHFHandheldCapability();
	private final LazyOptional<IVHFHandheldCapability> optionalData = LazyOptional.of(() -> backend);

	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		return RadiocraftCapabilities.VHF_HANDHELDS.orEmpty(cap, optionalData);
	}

	@Override
	public CompoundTag serializeNBT() {
		return backend.serializeNBT();
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		backend.deserializeNBT(nbt);
	}

}
