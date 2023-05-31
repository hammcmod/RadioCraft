package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.api.capabilities.IAntennaWireHolderCapability;
import com.arrl.radiocraft.api.capabilities.RadiocraftCapabilities;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class AntennaWireHolderCapabilityProvider implements ICapabilityProvider {

	private final IAntennaWireHolderCapability backend = new AntennaWireHolderCapability();
	private final LazyOptional<IAntennaWireHolderCapability> optionalData = LazyOptional.of(() -> backend);

	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		return RadiocraftCapabilities.ANTENNA_WIRE_HOLDERS.orEmpty(cap, optionalData);
	}


}
