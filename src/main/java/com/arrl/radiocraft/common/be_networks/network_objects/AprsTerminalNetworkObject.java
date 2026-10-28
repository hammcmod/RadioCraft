package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.capabilities.BasicEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The H19 terminal on the power wire: a {@link MicrowaveNetworkObjects.StationLink} (a microwave radio on the same
 * wire serves it) that also draws {@code aprs_terminal_power_use} a tick, always, as a mains-fed terminal does. Without
 * that it is dark: nothing boots and nothing shows (maintainer, 2026-10-27). It still needs a powered radio reachable
 * through the station gear to have anything to say ({@code AprsTerminalBlockEntity#isOn}).
 */
public class AprsTerminalNetworkObject extends MicrowaveNetworkObjects.StationLink {

	public static final ResourceLocation TYPE = Radiocraft.id("aprs_terminal");

	/** Whether the wire fed it this tick. */
	public boolean isPowered = false;

	public AprsTerminalNetworkObject(Level level, BlockPos pos) {
		super(level, pos);
		energyStorage = new BasicEnergyStorage(bufferSize(), bufferSize(), bufferSize());
	}

	/** One tick's draw: what the power network tops up each tick. */
	private static int bufferSize() {
		try {
			return Math.max(1, RadiocraftServerConfig.APRS_TERMINAL_POWER_USE.get());
		}
		catch(IllegalStateException e) { // Config not loaded yet.
			return 3;
		}
	}

	@Override
	public boolean isIndirectConsumer() {
		return true;
	}

	@Override
	public void tick(Level level, BlockPos pos) {
		isPowered = !RadiocraftServerConfig.RADIO_POWER_REQUIRED.get() || tryConsumeEnergy(RadiocraftServerConfig.APRS_TERMINAL_POWER_USE.get(), false);
	}

	@Override
	public ResourceLocation getType() {
		return TYPE;
	}
}
