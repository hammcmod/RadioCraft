package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RepeaterNetworkObject;
import com.arrl.radiocraft.common.blocks.StackedBlocks;
import com.arrl.radiocraft.common.blocks.radios.CommercialRepeaterBlock;
import com.arrl.radiocraft.common.capabilities.BasicEnergyStorage;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The commercial repeater's works, in the bottom cell of its rack: a VHF repeater ({@link VHFRepeaterBlockEntity}) with
 * its duplexer built in, transmitting {@link #POWER} times as strong as the VHF repeater and drawing that much more to
 * do it. The rack's bottom cell is its own battery and charge controller ({@link Commercial}): the station's power
 * wire, run to any cell of the rack (see {@code WireUtils#wireLinksOf}), may come from batteries or straight from solar
 * panels, and the repeater rides out a brief outage on what it has stored. Coax from it still goes to an antenna. Its
 * lamps are on the rack: power, receive and transmit on the radio (cell 1), and the controller's beacon (cell 3) while
 * it is on, blinking while it runs on its battery with nothing coming in.
 */
public class CommercialRepeaterBlockEntity extends VHFRepeaterBlockEntity {

	public static final float POWER = 2.0F;
	/** The controller beacon's blink while on battery: on and off this many ticks each. */
	private static final int BLINK_TICKS = 10;

	public CommercialRepeaterBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.COMMERCIAL_REPEATER.get(), pos, state);
	}

	@Override
	protected boolean hasBuiltInDuplexer() {
		return true;
	}

	@Override
	protected float transmitPower() {
		return POWER;
	}

	@Override
	protected void additionalTick() {
		super.additionalTick();
		if(level == null || level.isClientSide)
			return;
		boolean on = isPowered();
		boolean onBattery = on && getNetworkObject(level, worldPosition) instanceof Commercial works && works.isOnBattery();
		lamps(worldPosition.above(1), on, on && isInputActive(), on && isTransmitting());
		lamps(worldPosition.above(3), on && (!onBattery || (level.getGameTime() / BLINK_TICKS) % 2 == 0), false, false);
	}

	private void lamps(BlockPos pos, boolean pwr, boolean rx, boolean tx) {
		BlockState state = level.getBlockState(pos);
		if(!(state.getBlock() instanceof CommercialRepeaterBlock) || state.getValue(StackedBlocks.PART) == 0)
			return;
		BlockState lit = state.setValue(CommercialRepeaterBlock.LAMP_PWR, pwr).setValue(CommercialRepeaterBlock.LAMP_RX, rx).setValue(CommercialRepeaterBlock.LAMP_TX, tx);
		if(lit != state)
			level.setBlock(pos, lit, Block.UPDATE_CLIENTS);
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new Commercial(level, worldPosition, Math.round(CommonConfig.VHF_REPEATER_TRANSMIT_TICK.get() * POWER), CommonConfig.VHF_REPEATER_RECEIVE_TICK.get());
	}

	/**
	 * The repeater on the power wire and the coax, drawing a VHF repeater's receive and {@link #POWER} times its
	 * transmit, from a battery of its own ({@code commercial_repeater_battery}) that its built-in charge controller
	 * fills at {@code commercial_repeater_charge_rate} a tick from whatever the wire offers: batteries and charge
	 * controllers (an indirect consumer) or solar panels directly (a direct consumer). Pull the wire and it runs on
	 * until the battery is flat.
	 */
	public static class Commercial extends RepeaterNetworkObject {

		public static final ResourceLocation TYPE = com.arrl.radiocraft.Radiocraft.id("commercial_repeater");

		/** Ticks without anything coming in over the wire before it counts as running on its battery. */
		public static final int ON_BATTERY_AFTER_TICKS = 20;
		/** What the battery held at the end of the last tick, to tell charge coming in from the draw going out. */
		private int storedAfterLastTick = 0;
		/** Game time charge last came in over the wire. */
		private long lastChargeTime = Long.MIN_VALUE;

		public Commercial(Level level, BlockPos pos, int transmitUse, int receiveUse) {
			super(level, pos, transmitUse, receiveUse);
			energyStorage = new BasicEnergyStorage(battery(), chargeRate(), Math.max(transmitUse, 1));
		}

		private static int battery() {
			try {
				return Math.max(1, CommonConfig.COMMERCIAL_REPEATER_BATTERY.get());
			}
			catch(IllegalStateException e) { // Config not loaded yet.
				return 100_000;
			}
		}

		private static int chargeRate() {
			try {
				return Math.max(1, CommonConfig.COMMERCIAL_REPEATER_CHARGE_RATE.get());
			}
			catch(IllegalStateException e) {
				return 60;
			}
		}

		/** Takes solar power as it comes, like a charge controller. */
		@Override
		public boolean isDirectConsumer() {
			return true;
		}

		@Override
		public void resolvePower() {
			long now = level == null ? 0L : level.getGameTime();
			if(energyStorage.getEnergyStored() > storedAfterLastTick || lastChargeTime == Long.MIN_VALUE)
				lastChargeTime = now; // Something came in since last tick (or we just appeared: give the wire a second).
			super.resolvePower();
			storedAfterLastTick = energyStorage.getEnergyStored();
		}

		/** True while it runs on its own battery with nothing having come in over the wire for a second. */
		public boolean isOnBattery() {
			return isPowered && level != null && level.getGameTime() - lastChargeTime > ON_BATTERY_AFTER_TICKS;
		}

		/** Its own battery counts, so the front panel's PWR cycle only shows once that is flat as well. */
		@Override
		public boolean hasBattery() {
			return energyStorage.getEnergyStored() > 0 || super.hasBattery();
		}

		@Override
		public ResourceLocation getType() {
			return TYPE;
		}
	}
}
