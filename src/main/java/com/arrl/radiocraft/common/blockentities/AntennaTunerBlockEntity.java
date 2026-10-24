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
import com.arrl.radiocraft.common.radio.TunerMath;
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
 * The manual antenna tuner. Placed anywhere on the coax between a radio and its antenna, its three dials match an
 * antenna that is off resonance for the radio's frequency once each is turned to the right reading (see
 * {@link AntennaTunerNetworkObject}, which owns the dials, and {@link AntennaNetworkObject#getSWR}, which applies the
 * match). This block entity owns the screen: the readout and the lamp that guides the player to the match.
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
	public static final int STATUS_TUNING = 7;

	public static final int DATA_STATUS = 0;
	public static final int DATA_RAW_SWR = 1;
	public static final int DATA_TUNED_SWR = 2;
	public static final int DATA_DIAL_FIRST = 3;
	public static final int DATA_COUNT = 6;

	/**
	 * What the tuner is doing right now.
	 *
	 * @param status One of the {@code STATUS_} constants.
	 * @param rawSwr The antenna's own SWR at the radio's frequency, 0 without one.
	 * @param tunedSwr The SWR the radio sees through the dials as set, 0 without an antenna.
	 */
	public record Reading(int status, double rawSwr, double tunedSwr) {
		public static final Reading NO_RADIO = new Reading(STATUS_NO_RADIO, 0.0D, 0.0D);
	}

	/** Synced to the open screen: SWRs as hundredths, since menu data slots carry shorts. */
	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			if(index >= DATA_DIAL_FIRST && index < DATA_DIAL_FIRST + 3)
				return getDial(index - DATA_DIAL_FIRST);
			Reading reading = read();
			return switch(index) {
				case DATA_STATUS -> reading.status();
				case DATA_RAW_SWR -> (int)Math.round(Math.min(reading.rawSwr(), 300.0D) * 100.0D);
				case DATA_TUNED_SWR -> (int)Math.round(Math.min(reading.tunedSwr(), 300.0D) * 100.0D);
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

	@Nullable
	private AntennaTunerNetworkObject tuner() {
		return level != null && IBENetworks.getObject(level, worldPosition) instanceof AntennaTunerNetworkObject tuner ? tuner : null;
	}

	/** One of the three dials, 0 to {@link TunerMath#DIAL_MAX} (server side; 0 before the tuner is on a network). */
	public int getDial(int index) {
		AntennaTunerNetworkObject tuner = tuner();
		return tuner == null ? 0 : tuner.getDial(index);
	}

	/** Turns a dial (from the screen's packet). */
	public void setDial(int index, int value) {
		AntennaTunerNetworkObject tuner = tuner();
		if(tuner != null) {
			tuner.setDial(index, value);
			setChanged();
		}
	}

	/**
	 * Server side: the tuner's view of its coax run. The first powered radio on the run sets the frequency; a radio
	 * that is off, or a run without a radio or antenna, reports as such.
	 */
	public Reading read() {
		if(level == null || level.isClientSide)
			return Reading.NO_RADIO;
		AntennaTunerNetworkObject tuner = tuner();
		if(tuner == null)
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

		float frequency = radio.getSwrFrequency();
		double raw = antenna.getRawSWR(frequency);
		if(antennas.size() > 1 || antenna.isOverdrawn())
			return new Reading(STATUS_OVERDRAWN, raw, raw);
		if(!RadiocraftServerConfig.ANTENNA_TUNER_ENABLED.get())
			return new Reading(STATUS_DISABLED, raw, raw);
		double tuned = tuner.match(antenna, raw, frequency).swr();
		int status = tuned <= TunerMath.PERFECT_SWR ? STATUS_MATCHED
				: raw > RadiocraftServerConfig.ANTENNA_TUNER_MAX_SWR.get() ? STATUS_OUT_OF_RANGE : STATUS_TUNING;
		return new Reading(status, raw, tuned);
	}

}
