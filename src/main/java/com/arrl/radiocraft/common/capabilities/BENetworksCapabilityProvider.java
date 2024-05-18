package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.api.capabilities.RadiocraftCapabilities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class BENetworksCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

	private final IBENetworks backend;
	private final LazyOptional<IBENetworks> optionalData;

	public BENetworksCapabilityProvider(Level level) {
		backend = new BENetworksCapability(level);
		optionalData = LazyOptional.of(() -> backend);
	}

	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		return RadiocraftCapabilities.BE_NETWORKS.orEmpty(cap, optionalData);
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
