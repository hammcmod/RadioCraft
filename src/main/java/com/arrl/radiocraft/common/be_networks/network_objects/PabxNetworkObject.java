package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.capabilities.BasicEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The phone PABX on the power wire: draws {@code pabx_power_use} a tick, always, as a mains-fed exchange does. Without
 * it, the phones it serves have no line (see {@link com.arrl.radiocraft.common.blockentities.PabxBlockEntity}).
 */
public class PabxNetworkObject extends MicrowaveNetworkObjects.StationLink {

	public static final ResourceLocation TYPE = Radiocraft.id("pabx");

	/** Whether the wire fed it this tick. */
	public boolean isPowered = false;

	public PabxNetworkObject(Level level, BlockPos pos) {
		super(level, pos);
		energyStorage = new BasicEnergyStorage(bufferSize(), bufferSize(), bufferSize());
	}

	/** One tick's draw: what the power network tops up each tick. */
	private static int bufferSize() {
		try {
			return Math.max(1, RadiocraftServerConfig.PABX_POWER_USE.get());
		}
		catch(IllegalStateException e) { // Config not loaded yet.
			return 2;
		}
	}

	@Override
	public boolean isIndirectConsumer() {
		return true;
	}

	@Override
	public void tick(Level level, BlockPos pos) {
		isPowered = !RadiocraftServerConfig.RADIO_POWER_REQUIRED.get() || tryConsumeEnergy(RadiocraftServerConfig.PABX_POWER_USE.get(), false);
	}

	@Override
	public ResourceLocation getType() {
		return TYPE;
	}
}
