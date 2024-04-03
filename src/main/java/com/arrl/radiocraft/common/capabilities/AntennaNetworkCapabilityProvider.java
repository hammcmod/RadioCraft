package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.api.capabilities.IAntennaNetworkCapability;
import com.arrl.radiocraft.api.capabilities.RadiocraftCapabilities;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class AntennaNetworkCapabilityProvider implements ICapabilityProvider {

	private final IAntennaNetworkCapability backend = new AntennaNetworkCapability();
	private final LazyOptional<IAntennaNetworkCapability> optionalData = LazyOptional.of(() -> backend);

	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		return RadiocraftCapabilities.ANTENNA_NETWORKS.orEmpty(cap, optionalData);
	}


}
