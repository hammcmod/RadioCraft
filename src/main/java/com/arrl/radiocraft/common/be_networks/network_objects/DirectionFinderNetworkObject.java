package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.capabilities.BasicEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The direction finder on the power wire: a {@link MicrowaveNetworkObjects.StationLink} (a microwave radio on the same
 * wire serves it) that also draws {@code direction_finder_power_use} a tick while switched on. Without that it stays
 * dark, like a radio with no battery. Power can come in through any of its modules, which pass it on by touching
 * ({@link DirectionFinderModuleNetworkObject}).
 */
public class DirectionFinderNetworkObject extends MicrowaveNetworkObjects.StationLink {

	public static final ResourceLocation TYPE = Radiocraft.id("direction_finder");

	/** The POWER button, and whether it really runs. */
	public boolean wantsOn = false;
	public boolean isPowered = false;

	public DirectionFinderNetworkObject(Level level, BlockPos pos) {
		super(level, pos);
		energyStorage = new BasicEnergyStorage(bufferSize(), bufferSize(), bufferSize());
	}

	/** One tick's draw: what the power network tops up each tick. */
	private static int bufferSize() {
		try {
			return Math.max(1, RadiocraftServerConfig.DIRECTION_FINDER_POWER_USE.get());
		}
		catch(IllegalStateException e) { // Config not loaded yet.
			return 6;
		}
	}

	@Override
	public boolean isIndirectConsumer() {
		return true;
	}

	@Override
	public void tick(Level level, BlockPos pos) {
		resolvePower();
	}

	/** Decides {@link #isPowered} now: switched on and able to pay this tick's draw. */
	public void resolvePower() {
		if(!wantsOn)
			isPowered = false;
		else if(!RadiocraftServerConfig.RADIO_POWER_REQUIRED.get())
			isPowered = true;
		else
			isPowered = tryConsumeEnergy(RadiocraftServerConfig.DIRECTION_FINDER_POWER_USE.get(), false);
	}

	/** Its modules hand power on to it by touching (see the module's {@code linksDirectlyTo}). */
	@Override
	public boolean linksDirectlyTo(BENetworkObject other, boolean power) {
		return power && other instanceof DirectionFinderModuleNetworkObject;
	}

	@Override
	public void save(CompoundTag nbt) {
		super.save(nbt);
		nbt.putBoolean("wantsOn", wantsOn);
	}

	@Override
	public void load(IBENetworks cap, CompoundTag nbt) {
		super.load(cap, nbt);
		wantsOn = nbt.getBoolean("wantsOn");
	}

	@Override
	public ResourceLocation getType() {
		return TYPE;
	}
}
