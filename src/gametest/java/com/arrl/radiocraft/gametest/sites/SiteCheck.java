package com.arrl.radiocraft.gametest.sites;

import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.AprsTerminalBlockEntity;
import com.arrl.radiocraft.common.blockentities.CommercialRadioModuleBlockEntity;
import com.arrl.radiocraft.common.blockentities.DirectionFinderBlockEntity;
import com.arrl.radiocraft.common.blockentities.IonosphericProcessorBlockEntity;
import com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What every station in a box is doing: on and powered, on an antenna and how well matched, the processor online,
 * the direction finder's modules ready, the batteries' charge. Anything that would leave a player's find not working
 * is a problem. {@code powered} false is for a site found without power (the buried site): flat batteries and dark
 * radios are then as built.
 */
public final class SiteCheck {

	/** Worst SWR a site's antenna may show on the station's own frequency. */
	public static final double MAX_SWR = 3.0D;

	public record Report(List<String> lines, List<String> problems) {}

	private SiteCheck() {}

	@Nullable
	private static BENetworkObject object(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof INetworkObjectProvider provider ? provider.getNetworkObject(level, pos) : IBENetworks.getObject(level, pos);
	}

	public static Report check(ServerLevel level, BlockPos from, BlockPos to, boolean powered) {
		List<String> lines = new ArrayList<>(), problems = new ArrayList<>();
		for(BlockPos pos : BlockPos.betweenClosed(from, to)) {
			BlockEntity entity = level.getBlockEntity(pos);
			if(entity == null)
				continue;
			String where = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).getPath() + " " + pos.toShortString() + ": ";
			String line = null;
			List<String> wrong = new ArrayList<>();
			if(entity instanceof RadioBlockEntity radio && object(level, pos) instanceof RadioNetworkObject network) {
				float hz = radio.getFrequency();
				StringBuilder antennas = new StringBuilder();
				for(AntennaNetworkObject antenna : network.getAntennas()) {
					double swr = antenna.getSWR(hz);
					antennas.append(antenna.getAntenna() == null ? "?" : antenna.getAntenna().getType().getId().getPath())
							.append(String.format(Locale.ROOT, " swr %.2f ", swr));
					if(swr > MAX_SWR)
						wrong.add("SWR " + String.format(Locale.ROOT, "%.2f", swr));
				}
				line = String.format(Locale.ROOT, "%.4f MHz on=%s powered=%s antennas: %s", hz / 1e6, network.wantsOn, network.isPowered, antennas);
				if(network.getAntennas().isEmpty())
					wrong.add("no antenna");
				if(powered && !network.isPowered)
					wrong.add("not running");
			}
			else if(entity instanceof MicrowaveRadioBlockEntity radio) {
				int dishes = radio.dishes(level).size();
				line = "powered=" + radio.isPowered() + " dishes=" + radio.dishes(level).stream().map(dish -> dish.getBlockPos().toShortString()).toList()
						+ " linked=" + radio.isLinked();
				if(dishes == 0)
					wrong.add("no dish or horn");
				if(powered && !radio.isPowered())
					wrong.add("not running");
			}
			else if(entity instanceof AprsTerminalBlockEntity terminal) {
				line = "powered=" + terminal.isPowered() + " radio=" + terminal.isRadioOn() + " " + terminal.getRadioFrequency() / 1e6 + " MHz callsign="
						+ terminal.getCallsign() + " beacon=" + terminal.isBeaconEnabled() + " wx=" + terminal.getWeatherStationDistance();
				if(powered && !terminal.isOn())
					wrong.add("not running");
				if(terminal.getCallsign().isEmpty())
					wrong.add("no callsign");
			}
			else if(entity instanceof IonosphericProcessorBlockEntity processor) {
				line = "online=" + processor.isOnline() + " ionosonde=" + processor.hasIonosonde() + " radios=" + processor.getRadios().size()
						+ " callsign=" + processor.getCallsign();
				if(!processor.hasIonosonde())
					wrong.add("no ionosonde");
				if(powered && !processor.isOnline())
					wrong.add("offline");
				if(processor.getRadios().isEmpty())
					wrong.add("no radio");
			}
			else if(entity instanceof DirectionFinderBlockEntity finder) {
				line = "on=" + finder.isOn() + " powered=" + finder.isPowered() + " " + finder.getFrequency() / 1e6 + " MHz antenna="
						+ finder.getAntennaType() + " modules=" + finder.getStatuses();
				if(powered && !finder.isOn())
					wrong.add("not running");
				if(finder.getStatuses().size() < 3)
					wrong.add("fewer than three modules");
				for(DirectionFinderBlockEntity.ModuleStatus status : finder.getStatuses())
					if(status != DirectionFinderBlockEntity.ModuleStatus.READY && status != DirectionFinderBlockEntity.ModuleStatus.LOCKED)
						wrong.add("a module is " + status);
			}
			else if(entity instanceof CommercialRadioModuleBlockEntity module)
				line = module.getKind() + " " + module.getBandName() + " " + module.getFrequency() / 1e6 + " MHz";
			else if(object(level, pos) instanceof BatteryNetworkObject battery) {
				line = "energy=" + battery.getStorage().getEnergyStored();
				if(powered && battery.getStorage().getEnergyStored() == 0)
					wrong.add("flat");
			}
			if(line == null)
				continue;
			lines.add(where + line + (wrong.isEmpty() ? "" : "  <-- " + String.join(", ", wrong)));
			for(String problem : wrong)
				problems.add(where + problem);
		}
		return new Report(lines, problems);
	}
}
