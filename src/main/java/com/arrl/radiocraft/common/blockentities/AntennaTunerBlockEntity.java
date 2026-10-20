package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaTunerNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.AntennaTunerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * An automatic antenna tuner. Placed anywhere on the coax between a radio and its antenna it matches an antenna that is
 * off resonance for the radio's frequency: the radio sees SWR 1 and most of the power the mismatch would reflect
 * reaches the antenna. The match itself is applied by {@link AntennaNetworkObject#getSWR} and
 * {@link AntennaNetworkObject#getMatchEfficiency}; this block entity only owns the tuner's presence on the coax and the
 * readout its screen shows.
 */
public class AntennaTunerBlockEntity extends PowerBlockEntity {

	/** Readout states, in the order the screen's language keys use them. */
	public static final int STATUS_NO_RADIO = 0;
	public static final int STATUS_RADIO_OFF = 1;
	public static final int STATUS_NO_ANTENNA = 2;
	public static final int STATUS_MATCHED = 3;
	public static final int STATUS_OUT_OF_RANGE = 4;
	public static final int STATUS_OVERDRAWN = 5;
	public static final int STATUS_DISABLED = 6;

	public static final int DATA_STATUS = 0;
	public static final int DATA_RAW_SWR = 1;
	public static final int DATA_TUNED_SWR = 2;
	public static final int DATA_COUNT = 3;

	/**
	 * What the tuner is doing right now.
	 *
	 * @param status One of the {@code STATUS_} constants.
	 * @param rawSwr The antenna's own SWR at the radio's frequency, 0 without one.
	 * @param tunedSwr The SWR the radio sees, 0 without an antenna.
	 */
	public record Reading(int status, double rawSwr, double tunedSwr) {
		public static final Reading NO_RADIO = new Reading(STATUS_NO_RADIO, 0.0D, 0.0D);
	}

	/** Synced to the open screen as hundredths of an SWR unit, since menu data slots carry ints. */
	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			Reading reading = read();
			return switch(index) {
				case DATA_STATUS -> reading.status();
				case DATA_RAW_SWR -> (int)Math.round(reading.rawSwr() * 100.0D);
				case DATA_TUNED_SWR -> (int)Math.round(reading.tunedSwr() * 100.0D);
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return DATA_COUNT;
		}
	};

	public AntennaTunerBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.ANTENNA_TUNER.get(), pos, state);
	}

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("container.radiocraft.antenna_tuner");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
		return new AntennaTunerMenu(id, this, data);
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new AntennaTunerNetworkObject(level, worldPosition);
	}

	/**
	 * Server side: the tuner's view of its coax run. The first powered radio on the run sets the frequency; a radio
	 * that is off, or a run without a radio or antenna, reports as such.
	 */
	public Reading read() {
		if(level == null || level.isClientSide || !(IBENetworks.getObject(level, worldPosition) instanceof AntennaTunerNetworkObject tuner))
			return Reading.NO_RADIO;

		List<RadioNetworkObject> radios = tuner.getRadios();
		if(radios.isEmpty())
			return Reading.NO_RADIO;

		RadioBlockEntity radio = null;
		for(RadioNetworkObject candidate : radios) {
			if(candidate.isPowered && level.getBlockEntity(candidate.getPos()) instanceof RadioBlockEntity be) {
				radio = be;
				break;
			}
		}
		if(radio == null)
			return new Reading(STATUS_RADIO_OFF, 0.0D, 0.0D);

		List<AntennaNetworkObject> antennas = tuner.getAntennas();
		AntennaNetworkObject antenna = antennas.isEmpty() ? null : antennas.getFirst();
		if(antenna == null || antenna.getAntenna() == null)
			return new Reading(STATUS_NO_ANTENNA, 0.0D, 0.0D);

		double raw = antenna.getRawSWR(radio.getFrequency());
		double tuned = antenna.getSWR(radio.getFrequency());
		if(antennas.size() > 1 || antenna.isOverdrawn())
			return new Reading(STATUS_OVERDRAWN, raw, tuned);
		if(!RadiocraftServerConfig.ANTENNA_TUNER_ENABLED.get())
			return new Reading(STATUS_DISABLED, raw, tuned);
		return new Reading(tuned <= 1.0D ? STATUS_MATCHED : STATUS_OUT_OF_RANGE, raw, tuned);
	}

}
