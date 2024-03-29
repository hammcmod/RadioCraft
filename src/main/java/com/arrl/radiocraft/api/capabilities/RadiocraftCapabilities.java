package com.arrl.radiocraft.api.capabilities;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class RadiocraftCapabilities {

	public static Capability<ICallsignCapability> CALLSIGNS = CapabilityManager.get(new CapabilityToken<>(){});
	public static Capability<IAntennaWireHolderCapability> ANTENNA_WIRE_HOLDERS = CapabilityManager.get(new CapabilityToken<>(){});

}
