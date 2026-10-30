package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.ICoaxNetworkObject;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The network objects of the microwave link: the radio, its dishes, and the stations wired to it. */
public final class MicrowaveNetworkObjects {

	private MicrowaveNetworkObjects() {}

	/** A microwave dish or horn on the coax. What it points at lives on its block entity. */
	public static class Dish extends BENetworkObject implements ICoaxNetworkObject {

		public static final ResourceLocation TYPE = Radiocraft.id("microwave_dish");

		public Dish(Level level, BlockPos pos) {
			super(level, pos);
		}

		@Override
		public ResourceLocation getType() {
			return TYPE;
		}
	}

	/**
	 * A microwave radio: on the power wire, where it draws {@code microwave_power_use} a tick while on, and on the coax
	 * to its dishes. The stations it serves are wired to it with data wire ({@code DataWire}).
	 */
	public static class Radio extends PowerNetworkObject implements ICoaxNetworkObject {

		public static final ResourceLocation TYPE = Radiocraft.id("microwave_radio");

		/** The power switch, and whether it really runs. */
		public boolean wantsOn = false;
		public boolean isPowered = false;

		public Radio(Level level, BlockPos pos) {
			super(level, pos, bufferSize(), bufferSize(), bufferSize());
		}

		/** One tick's draw: what the power network tops up each tick. */
		private static int bufferSize() {
			try {
				return Math.max(1, RadiocraftServerConfig.MICROWAVE_POWER_USE.get());
			}
			catch(IllegalStateException e) { // Config not loaded yet.
				return 26;
			}
		}

		@Override
		public boolean isIndirectConsumer() {
			return true;
		}

		@Override
		public boolean isDirectConsumer() {
			return false;
		}

		@Override
		public void tick(Level level, BlockPos pos) {
			if(!wantsOn)
				isPowered = false;
			else if(!RadiocraftServerConfig.RADIO_POWER_REQUIRED.get())
				isPowered = true;
			else
				isPowered = tryConsumeEnergy(RadiocraftServerConfig.MICROWAVE_POWER_USE.get(), false);
		}

		/** The dishes and horns on this radio's coax. */
		public List<BlockPos> dishes() {
			Set<BlockPos> out = new LinkedHashSet<>();
			for(BENetwork network : networks.values()) {
				if(network instanceof PowerBENetwork)
					continue;
				for(BENetworkObject object : network.getNetworkObjects())
					if(object instanceof Dish)
						out.add(object.getPos());
			}
			return new ArrayList<>(out);
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

	/**
	 * A station that a microwave radio could serve over the power wire: the H19, the direction finder and the
	 * telephone. Since 2026-10-30 that line is data wire ({@code DataWire}); the object stays so saved worlds load, and
	 * the direction finder still takes its power through it.
	 */
	public static class StationLink extends PowerNetworkObject {

		public static final ResourceLocation TYPE = Radiocraft.id("station_link");

		public StationLink(Level level, BlockPos pos) {
			super(level, pos, 0, 0, 0);
		}

		@Override
		public boolean isIndirectConsumer() {
			return false;
		}

		@Override
		public boolean isDirectConsumer() {
			return false;
		}

				@Override
		public ResourceLocation getType() {
			return TYPE;
		}
	}
}
