package com.arrl.radiocraft.common.commands;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.weather.WeatherSource;
import com.arrl.radiocraft.api.weather.WeatherSources;
import com.arrl.radiocraft.common.radio.weather.WeatherReports;
import com.arrl.radiocraft.common.radio.wefax.WefaxChart;
import com.arrl.radiocraft.common.radio.wefax.WefaxPrints;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.antenna.AntennaTypes;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import com.arrl.radiocraft.common.radio.rfi.PickupZone;
import com.arrl.radiocraft.common.radio.rfi.RedstoneInterference;
import com.arrl.radiocraft.common.radio.rfi.TransmitInterference;
import com.arrl.radiocraft.common.radio.solar.Aurora;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import com.arrl.radiocraft.common.radio.terrain.TerrainAtlas;
import com.arrl.radiocraft.common.radio.terrain.TerrainMath;
import com.arrl.radiocraft.common.radio.terrain.TerrainPropagation;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Developer diagnostics and controls, so the mod can be inspected and driven without a GUI. Output is plain text so it
 * reads well over RCON (see tools/rcon.py) as well as in chat. Requires permission level 2.
 * <p>
 * Read-only:
 * <ul>
 *     <li>/rcdebug radio &lt;pos&gt; - radio state, connected antennas and their SWR at the tuned frequency</li>
 *     <li>/rcdebug antenna &lt;pos&gt; - what antenna (if any) is recognized at pos, and its SWR per band</li>
 *     <li>/rcdebug network &lt;pos&gt; - the network object at pos, its networks and their members</li>
 *     <li>/rcdebug solar - current solar weather and time of day for this dimension</li>
 *     <li>/rcdebug terrain [reset] - terrain map size and what terrain has cost so far (reset clears the counters)</li>
 *     <li>/rcdebug terrain path &lt;from&gt; &lt;to&gt; &lt;band&gt; - what terrain does to a signal between two points</li>
 * </ul>
 * Changes the world (stand-ins for player actions):
 * <ul>
 *     <li>/rcdebug wire &lt;from&gt; &lt;to&gt; - string an antenna wire, as the antenna wire item would</li>
 *     <li>/rcdebug power &lt;pos&gt; on|off - radio power switch</li>
 *     <li>/rcdebug tune &lt;pos&gt; &lt;MHz&gt; - set a radio's frequency (clamped/stepped to its band)</li>
 *     <li>/rcdebug mode &lt;pos&gt; ssb|cw|off - radio mode buttons</li>
 *     <li>/rcdebug transmit &lt;pos&gt; - send one frame of tone from an SSB radio, as if someone spoke into it</li>
 *     <li>/rcdebug key &lt;pos&gt; - key one buffer of morse from a CW radio</li>
 *     <li>/rcdebug parrot &lt;pos&gt; &lt;seconds&gt;|off - make a radio echo what it receives after a delay</li>
 * </ul>
 */
public class DebugCommands {

	public static final LiteralArgumentBuilder<CommandSourceStack> BUILDER = Commands.literal("rcdebug")
			.requires(source -> source.hasPermission(2))
			.then(Commands.literal("radio").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(DebugCommands::radio)))
			.then(Commands.literal("antenna").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(DebugCommands::antenna)))
			.then(Commands.literal("network").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(DebugCommands::network)))
			.then(Commands.literal("microwave").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(DebugCommands::microwave)))
			.then(Commands.literal("sable").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(DebugCommands::sable)))
			.then(Commands.literal("wires").then(Commands.argument("from", BlockPosArgument.blockPos()).then(Commands.argument("to", BlockPosArgument.blockPos())
					.executes(DebugCommands::wires))))
			.then(Commands.literal("flipbundles").then(Commands.argument("from", BlockPosArgument.blockPos()).then(Commands.argument("to", BlockPosArgument.blockPos())
					.executes(DebugCommands::flipBundles))))
			.then(Commands.literal("tapes").executes(DebugCommands::tapes)
					.then(Commands.literal("give").then(Commands.argument("track", StringArgumentType.greedyString()).executes(DebugCommands::giveTape)))
					.then(Commands.literal("reload").executes(ctx -> {
						com.arrl.radiocraft.common.radio.tape.TapeLibrary.clearCache();
						return tapes(ctx);
					})))
			.then(Commands.literal("module").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(DebugCommands::module)
					.then(Commands.argument("program", StringArgumentType.word()).then(Commands.argument("mhz", DoubleArgumentType.doubleArg(0))
							.executes(DebugCommands::setModule)))))
			.then(Commands.literal("stations").executes(DebugCommands::stations))
			.then(Commands.literal("wx").executes(DebugCommands::wx)
					.then(Commands.literal("aprs").executes(DebugCommands::wxAprs)))
			.then(Commands.literal("wefax")
					.then(Commands.argument("chart", StringArgumentType.word())
							.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(WefaxChart.Type.values())
									.map(t -> t.name().toLowerCase(Locale.ROOT)), builder))
							.executes(DebugCommands::wefax)))
			.then(Commands.literal("solar").executes(DebugCommands::solar)
					.then(Commands.literal("set").then(Commands.argument("event", StringArgumentType.word())
							.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RadiocraftData.SOLAR_EVENTS.getKeys().stream().map(ResourceLocation::getPath), builder))
							.executes(DebugCommands::setSolar)))
					.then(Commands.literal("forecast").then(Commands.argument("event", StringArgumentType.word())
							.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RadiocraftData.SOLAR_EVENTS.getKeys().stream().map(ResourceLocation::getPath), builder))
							.executes(DebugCommands::forecastSolar)))
					.then(Commands.literal("aurora")
							.then(Commands.literal("on").executes(ctx -> aurora(ctx, true)))
							.then(Commands.literal("off").executes(ctx -> aurora(ctx, false))))
					.then(Commands.literal("sporadic_e")
							.then(Commands.literal("on").executes(ctx -> sporadicE(ctx, true)))
							.then(Commands.literal("off").executes(ctx -> sporadicE(ctx, false)))))
			.then(Commands.literal("wire").then(Commands.argument("from", BlockPosArgument.blockPos())
					.then(Commands.argument("to", BlockPosArgument.blockPos()).executes(DebugCommands::wire))))
			.then(Commands.literal("power").then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.literal("on").executes(ctx -> power(ctx, true)))
					.then(Commands.literal("off").executes(ctx -> power(ctx, false)))))
			.then(Commands.literal("tune").then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("mhz", DoubleArgumentType.doubleArg(0)).executes(DebugCommands::tune))))
			.then(Commands.literal("mode").then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.literal("ssb").executes(ctx -> mode(ctx, "ssb")))
					.then(Commands.literal("cw").executes(ctx -> mode(ctx, "cw")))
					.then(Commands.literal("off").executes(ctx -> mode(ctx, "off")))))
			.then(Commands.literal("transmit").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(DebugCommands::transmit)))
			.then(Commands.literal("key").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(DebugCommands::key)))
			.then(Commands.literal("terrain").executes(DebugCommands::terrain)
					.then(Commands.literal("reset").executes(DebugCommands::terrainReset))
					.then(Commands.literal("path").then(Commands.argument("from", BlockPosArgument.blockPos()).then(Commands.argument("to", BlockPosArgument.blockPos())
							.then(Commands.argument("band", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("2m", "10m", "20m", "40m", "80m"), b))
									.executes(DebugCommands::terrainPath))))))
			.then(Commands.literal("parrot").then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.literal("off").executes(ctx -> parrot(ctx, 0.0D)))
					.then(Commands.argument("seconds", DoubleArgumentType.doubleArg(0.05D, 60.0D))
							.executes(ctx -> parrot(ctx, DoubleArgumentType.getDouble(ctx, "seconds"))))));

	private static RadioBlockEntity getRadio(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
		return ctx.getSource().getLevel().getBlockEntity(pos) instanceof RadioBlockEntity radio ? radio : null;
	}

	private static int wire(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerLevel level = ctx.getSource().getLevel();
		BlockPos from = BlockPosArgument.getLoadedBlockPos(ctx, "from");
		BlockPos to = BlockPosArgument.getLoadedBlockPos(ctx, "to");
		if(from.equals(to))
			return fail(ctx, "Wire cannot start and end at the same place");
		AntennaWire wire = new AntennaWire(level, from);
		level.addFreshEntity(wire);
		wire.setEndPos(to);
		wire.updateAntennas();
		return send(ctx, List.of(String.format(Locale.ROOT, "wire %s -> %s length=%.2f (antennas recalculate after the update delay)",
				from.toShortString(), to.toShortString(), wire.getLength())));
	}

	private static int power(CommandContext<CommandSourceStack> ctx, boolean on) throws CommandSyntaxException {
		RadioBlockEntity radio = getRadio(ctx);
		if(radio == null)
			return fail(ctx, "No radio there");
		RadioNetworkObject net = radio.getNetworkObject(ctx.getSource().getLevel(), radio.getBlockPos()) instanceof RadioNetworkObject r ? r : null;
		if(net != null && net.wantsOn != on)
			radio.toggle();
		return send(ctx, List.of("switch=" + (net != null && net.wantsOn) + " powered=" + (net != null && net.isPowered)
				+ (net != null && net.isBrownedOut() ? " (browned out: not enough power on the network, see radios_need_power)" : "")));
	}

	private static int tune(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		RadioBlockEntity radio = getRadio(ctx);
		if(radio == null)
			return fail(ctx, "No radio there");
		radio.setFrequency((float)(DoubleArgumentType.getDouble(ctx, "mhz") * 1e6));
		radio.updateFrequency(0); // Clamps to the band and syncs to clients.
		return send(ctx, List.of(String.format(Locale.ROOT, "freq=%.3fMHz", radio.getFrequency() / 1e6)));
	}

	private static int mode(CommandContext<CommandSourceStack> ctx, String mode) throws CommandSyntaxException {
		RadioBlockEntity radio = getRadio(ctx);
		if(radio == null)
			return fail(ctx, "No radio there");
		switch(mode) {
			case "ssb" -> radio.setSSBEnabled(true);
			case "cw" -> {
				if(!(radio instanceof HFRadioBlockEntity hf))
					return fail(ctx, "Only HF radios have CW");
				hf.setCWEnabled(true);
			}
			default -> {
				radio.setSSBEnabled(false);
				if(radio instanceof HFRadioBlockEntity hf)
					hf.setCWEnabled(false);
			}
		}
		return send(ctx, List.of("mode=" + mode));
	}

	/**
	 * Pushes one 20 ms frame of a 440 Hz tone through the radio as if its operator had spoken into it. Other radios on
	 * the frequency show it as lastSignal in {@code /rcdebug radio}; with a voice chat server running and a client in
	 * earshot of the receiving radio, they also hear a blip.
	 */
	private static int transmit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		RadioBlockEntity radio = getRadio(ctx);
		if(radio == null)
			return fail(ctx, "No radio there");
		if(!radio.getSSBEnabled())
			return fail(ctx, "Radio is not in SSB mode");
		if(!(radio.getNetworkObject(ctx.getSource().getLevel(), radio.getBlockPos()) instanceof RadioNetworkObject net) || !net.isPowered)
			return fail(ctx, "Radio is off");

		short[] audio = new short[960]; // One Simple Voice Chat frame: 20 ms at 48 kHz.
		for(int i = 0; i < audio.length; i++)
			audio[i] = (short)(Math.sin(2 * Math.PI * 440 * i / 48000.0D) * 8000);
		ServerLevel level = ctx.getSource().getLevel();
		radio.acceptVoicePacket(() -> level, audio, UUID.nameUUIDFromBytes("rcdebug".getBytes(StandardCharsets.UTF_8)));
		return send(ctx, List.of("transmitted one voice frame from " + radio.getBlockPos().toShortString() + " on "
				+ String.format(Locale.ROOT, "%.3fMHz", radio.getFrequency() / 1e6) + " (check lastSignal on the other radio)"));
	}

	/** Keys one buffer of morse (a dit, a gap and a dah) from an HF radio in CW mode. */
	private static int key(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		RadioBlockEntity radio = getRadio(ctx);
		if(!(radio instanceof HFRadioBlockEntity hf))
			return fail(ctx, "No HF radio there");
		if(!hf.canTransmitCW())
			return fail(ctx, "Radio must be on and in CW mode");

		boolean[] key = new boolean[CWBuffer.BUFFER_LENGTH];
		for(int i = 0; i < CWBuffer.BUFFER_LENGTH; i++)
			key[i] = i < 3 || (i >= 6 && i < 15);
		hf.transmitMorse(List.of(new CWBuffer(0, key), new CWBuffer(1, new boolean[CWBuffer.BUFFER_LENGTH])));
		return send(ctx, List.of("keyed one morse buffer from " + hf.getBlockPos().toShortString() + " on "
				+ String.format(Locale.ROOT, "%.3fMHz", hf.getFrequency() / 1e6) + " (check lastSignal on the other radio)"));
	}

	/**
	 * Makes a radio echo everything it receives after a delay (a simplex "parrot" repeater), so one player can talk or
	 * key into a station and hear it come back from another one. {@code off} stops it.
	 */
	private static int parrot(CommandContext<CommandSourceStack> ctx, double seconds) throws CommandSyntaxException {
		RadioBlockEntity radio = getRadio(ctx);
		if(radio == null)
			return fail(ctx, "No radio there");
		int ticks = (int)Math.round(seconds * 20);
		radio.setParrotDelay(ticks);
		return send(ctx, List.of(ticks == 0 ? "parrot off" : String.format(Locale.ROOT,
				"parrot on: %s echoes what it receives after %.2fs (%d ticks). It must be on and in the mode you want echoed.",
				radio.getBlockPos().toShortString(), ticks / 20.0D, ticks)));
	}

	private static int radio(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerLevel level = ctx.getSource().getLevel();
		BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
		if(!(level.getBlockEntity(pos) instanceof RadioBlockEntity radio))
			return fail(ctx, "No radio at " + pos.toShortString());

		List<String> out = new ArrayList<>();
		Band band = radio.getBand();
		out.add("radio " + pos.toShortString() + " " + radio.getClass().getSimpleName());
		out.add(String.format(Locale.ROOT, "band=%s freq=%.3fMHz range=%.3f-%.3fMHz", band == null ? "none" : band.name(),
				radio.getFrequency() / 1e6, band == null ? 0 : band.minFrequency() / 1e6, band == null ? 0 : band.maxFrequency() / 1e6));

		String mode = radio.getSSBEnabled() ? "SSB" : "off";
		if(radio instanceof HFRadioBlockEntity hf && hf.getCWEnabled())
			mode = "CW";
		RadioNetworkObject net = radio.getNetworkObject(level, pos) instanceof RadioNetworkObject r ? r : null;
		out.add(String.format(Locale.ROOT, "powered=%s mode=%s ptt=%s transmitting=%s receiving=%s",
				net != null && net.isPowered, mode, radio.isPTTDown(), radio.canTransmitVoice(), radio.getVoiceReceiver().isReceiving()));
		out.add(String.format(Locale.ROOT, "speakerGain=%.2f micGain=%.2f measuredSwr=%.2f staticFactor=%.3f syncedPowered=%s",
				radio.getSpeakerGain(), radio.getMicGain(), radio.getAntennaSWR(), radio.getStaticVolume(), radio.wasPowered()));
		PickupZone zone = RedstoneInterference.getZone(level, pos);
		out.add(String.format(Locale.ROOT, "interference=%.3f rfiZone=%s", radio.getInterference(),
				zone == null ? "none" : zone.feedline().size() + " feedline + " + zone.antenna().size() + " antenna points, " + zone.min().toShortString() + " to " + zone.max().toShortString()));
		TransmitInterference.Targets targets = radio.getTransmitTargets();
		if(targets != null)
			out.add(String.format(Locale.ROOT, "transmitting: inducing level in %d dust block(s), flipped %d switch(es)%s", targets.dust().size(), targets.switches().size(),
					targets.dust().isEmpty() ? "" : " at " + targets.dust().keySet().stream().limit(6).map(BlockPos::toShortString).toList()));
		String parrot = radio.isParrot() ? String.format(Locale.ROOT, " parrot=%.2fs", radio.getParrotDelay() / 20.0D) : "";
		if(radio.getLastSignalGameTime() < 0)
			out.add("lastSignal=none meter=0" + parrot);
		else
			out.add(String.format(Locale.ROOT, "lastSignal=%.3f receivedTicksAgo=%d meter=%d%s", radio.getLastSignalStrength(),
					level.getGameTime() - radio.getLastSignalGameTime(), radio.getMeterReading(), parrot));

		List<AntennaNetworkObject> antennas = net == null ? List.of() : net.getAntennas();
		out.add("antennas=" + antennas.size() + (antennas.size() > 1 ? " (more than one antenna = overdraw)" : ""));
		for(AntennaNetworkObject antenna : antennas) {
			StaticAntenna<?> a = antenna.getAntenna();
			out.add(String.format(Locale.ROOT, "  %s %s swr=%.2f%s", antenna.getPos().toShortString(),
					a == null ? "unrecognized" : a.type.getId().toString(), a == null ? 0.0D : antenna.getSWR(radio.getFrequency()),
					antenna.hasTuner() ? String.format(Locale.ROOT, " (tuner: antenna swr=%.2f, power to antenna=%.0f%%)", antenna.getRawSWR(radio.getFrequency()), antenna.getMatchEfficiency(radio.getFrequency()) * 100.0D) : ""));
			out.add("    " + antenna.diagnose());
		}
		return send(ctx, out);
	}

	private static int antenna(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerLevel level = ctx.getSource().getLevel();
		BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
		StaticAntenna<?> antenna = AntennaTypes.match(level, pos);
		if(antenna == null)
			return fail(ctx, "No antenna recognized at " + pos.toShortString() + " (block: " + level.getBlockState(pos).getBlock() + ")");

		List<String> out = new ArrayList<>();
		out.add("antenna " + pos.toShortString() + " type=" + antenna.type.getId());
		out.add("data=" + antenna.data.serializeNBT(level.registryAccess()));
		for(Band band : Band.getDefaults()) {
			Band live = Band.getBand(band.name());
			if(live == null)
				continue;
			float centre = (live.minFrequency() + live.maxFrequency()) / 2.0F;
			out.add(String.format(Locale.ROOT, "  %-4s %.3fMHz swr=%.2f", live.name(), centre / 1e6, antenna.getSWR(centre)));
		}
		if(IBENetworks.getObject(level, pos) instanceof AntennaNetworkObject obj) {
			out.add("networkObject antenna=" + (obj.getAntenna() == null ? "none (not yet recalculated?)" : obj.getAntenna().type.getId()));
			out.add(obj.diagnose());
		}
		return send(ctx, out);
	}

	/**
	 * A microwave radio's dishes and every other radio it could hear, with why the signal is what it is: the screens only
	 * show the signal level (a radio cannot know why the far end is quiet), this says out of range, not facing, blocked.
	 */
	private static int microwave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		net.minecraft.server.level.ServerLevel level = ctx.getSource().getLevel();
		BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
		if(!(level.getBlockEntity(pos) instanceof com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity radio))
			return fail(ctx, "No microwave radio there");
		List<String> lines = new ArrayList<>();
		List<com.arrl.radiocraft.common.blockentities.MicrowaveDishBlockEntity> dishes = radio.dishes(level);
		lines.add(String.format(Locale.ROOT, "%s %d MHz phase=%s powered=%s rssi=%.1f%s", radio.identity(), radio.getFrequency(), radio.getPhase(), radio.isPowered(),
				radio.getRssi(), radio.getAlignReason().isEmpty() ? "" : " (" + radio.getAlignReason() + ")"));
		for(com.arrl.radiocraft.common.blockentities.MicrowaveDishBlockEntity dish : dishes)
			lines.add(String.format(Locale.ROOT, "  %s at %s az=%.1f el=%.1f range=%.0f", dish.isHorn() ? "horn" : "dish", dish.getBlockPos().toShortString(),
					dish.getAzimuth(), dish.getElevation(), dish.getRange()));
		if(dishes.isEmpty())
			lines.add("  no dish on its coax");
		for(com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity other : com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity.loaded(level)) {
			if(other == radio)
				continue;
			com.arrl.radiocraft.common.radio.microwave.MicrowaveMath.Signal signal = com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity.bestSignal(level, radio, other);
			lines.add(String.format(Locale.ROOT, "%s at %s %d MHz powered=%s phase=%s: %s rssi=%.1f", other.identity(), other.getBlockPos().toShortString(),
					other.getFrequency(), other.isPowered(), other.getPhase(), signal.result(), signal.rssi()));
			for(com.arrl.radiocraft.common.blockentities.MicrowaveDishBlockEntity mine : dishes)
				for(com.arrl.radiocraft.common.blockentities.MicrowaveDishBlockEntity theirs : other.dishes(level)) {
					com.arrl.radiocraft.common.radio.microwave.MicrowaveMath.End a = mine.end(), b = theirs.end();
					lines.add(String.format(Locale.ROOT, "  %s -> %s: %.0f blocks, off axis %.0f / %.0f deg, path %s",
							mine.getBlockPos().toShortString(), theirs.getBlockPos().toShortString(), Math.hypot(b.x() - a.x(), b.z() - a.z()),
							com.arrl.radiocraft.common.radio.microwave.MicrowaveMath.offAxis(a, b.x(), b.y(), b.z()),
							com.arrl.radiocraft.common.radio.microwave.MicrowaveMath.offAxis(b, a.x(), a.y(), a.z()),
							com.arrl.radiocraft.common.radio.microwave.MicrowavePaths.in(level).clear(a, b) ? "clear" : "BLOCKED"));
				}
		}
		return send(ctx, lines);
	}

	/** Checks the cable drawn in a box: strands that do not meet across a face, or run through each other (WireGeometry). */
	private static int wires(CommandContext<CommandSourceStack> ctx) {
		BlockPos from = BlockPosArgument.getBlockPos(ctx, "from"), to = BlockPosArgument.getBlockPos(ctx, "to");
		List<String> problems = com.arrl.radiocraft.common.blocks.WireGeometry.check(ctx.getSource().getLevel(), from, to);
		List<String> out = new ArrayList<>();
		if(from.equals(to)) // One block: every strand's look, pieces and centre line, to see why.
			out.addAll(com.arrl.radiocraft.common.blocks.WireGeometry.describe(ctx.getSource().getLevel(), from));
		out.add(problems.isEmpty() ? "Cable checks out in " + from.toShortString() + " to " + to.toShortString() : problems.size() + " problems:");
		out.addAll(problems.subList(0, Math.min(60, problems.size())));
		return send(ctx, out);
	}

	/**
	 * Turns every bundle in the box round on both axes, without solving again (every link and bend still holds, since
	 * all turn together), and prints the strands running through each other before and after: to see whether the
	 * solver ({@link com.arrl.radiocraft.common.blocks.BundleOrientation}) picked the better way. Any change nearby
	 * solves again.
	 */
	private static int flipBundles(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		BlockPos from = BlockPosArgument.getBlockPos(ctx, "from"), to = BlockPosArgument.getBlockPos(ctx, "to");
		List<BlockPos> bundles = new ArrayList<>();
		for(BlockPos pos : BlockPos.betweenClosed(from, to))
			if(level.getBlockEntity(pos) instanceof com.arrl.radiocraft.common.blockentities.WireBundleBlockEntity)
				bundles.add(pos.immutable());
		int before = com.arrl.radiocraft.common.blocks.WireGeometry.crossings(level, bundles);
		for(BlockPos pos : bundles) {
			var bundle = (com.arrl.radiocraft.common.blockentities.WireBundleBlockEntity)level.getBlockEntity(pos);
			bundle.setFlips(!bundle.flipX(), !bundle.flipZ());
			level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
		}
		int after = com.arrl.radiocraft.common.blocks.WireGeometry.crossings(level, bundles);
		return send(ctx, List.of(bundles.size() + " bundles turned round: " + before + " crossings before, " + after + " after"));
	}

	/** Whether the block at a position is on a Sable sub-level, and where it really is (compat/sable/Sublevels). */
	private static int sable(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
		var ship = com.arrl.radiocraft.compat.sable.Sublevels.at(level, pos);
		if(ship == null)
			return send(ctx, List.of(pos.toShortString() + " is not on a sub-level"
					+ (net.neoforged.fml.ModList.get().isLoaded("sable") ? "" : " (Sable is not installed)")));
		net.minecraft.world.phys.Vec3 at = com.arrl.radiocraft.compat.sable.Sublevels.worldCenter(level, pos);
		return send(ctx, List.of(pos.toShortString() + " is on sub-level " + (ship.getName() == null ? ship.getUniqueId() : ship.getName()),
				String.format(Locale.ROOT, "really at %.2f, %.2f, %.2f (block %s)", at.x, at.y, at.z,
						com.arrl.radiocraft.compat.sable.Sublevels.world(level, pos).toShortString())));
	}

	private static int network(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerLevel level = ctx.getSource().getLevel();
		BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
		BlockEntity be = level.getBlockEntity(pos);
		BENetworkObject obj = be instanceof INetworkObjectProvider provider ? provider.getNetworkObject(level, pos) : IBENetworks.getObject(level, pos);
		if(obj == null)
			return fail(ctx, "No network object at " + pos.toShortString());

		List<String> out = new ArrayList<>();
		out.add("networkObject " + pos.toShortString() + " " + obj.getClass().getSimpleName() + " type=" + obj.getType());
		if(obj instanceof PowerNetworkObject power)
			out.add(String.format(Locale.ROOT, "energy=%d/%d", power.getStorage().getEnergyStored(), power.getStorage().getMaxEnergyStored()));
		for(Direction dir : Direction.values())
			for(boolean power : new boolean[] { true, false }) {
				BENetwork network = obj.getNetwork(dir, power);
				if(network == null)
					continue;
				StringBuilder members = new StringBuilder();
				for(BENetworkObject member : network.getNetworkObjects())
					members.append(' ').append(member.getClass().getSimpleName().replace("NetworkObject", "")).append('@').append(member.getPos().toShortString());
				out.add(String.format(Locale.ROOT, "  %-5s %s %s members:%s", dir.getName(), network.getType().getPath(),
						network.getUUID().toString().substring(0, 8), members));
			}
		return send(ctx, out);
	}

	/** The text WX report for where you stand, from the level's weather source. */
	private static int wx(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		WeatherSource source = WeatherSources.get(level);
		List<String> out = new ArrayList<>();
		out.add("source=" + source.name() + " local=" + source.isLocal());
		out.addAll(WeatherReports.lines(level, source, BlockPos.containing(ctx.getSource().getPosition()), ""));
		return send(ctx, out);
	}

	/** The APRS weather packet a station where you stand would send. */
	private static int wxAprs(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		WeatherSource source = WeatherSources.get(level);
		var report = source.report(level, BlockPos.containing(ctx.getSource().getPosition()));
		String packet = WeatherReports.aprsWeather(level, report, report.windSpeed() * 1.3, report.rain() * 8);
		return send(ctx, List.of(packet));
	}

	/** Prints a WEFAX chart of where you stand into your inventory (until the WEFAX receiver has its screen). */
	private static int wefax(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		WefaxChart.Type type = WefaxChart.type(StringArgumentType.getString(ctx, "chart"));
		if(type == null)
			return fail(ctx, "Charts: radar, forecast, surface");
		ServerLevel level = ctx.getSource().getLevel();
		var player = ctx.getSource().getPlayerOrException();
		var stack = WefaxPrints.print(level, WeatherSources.get(level), player.blockPosition(), type, "");
		if(!player.getInventory().add(stack))
			player.drop(stack, false);
		return send(ctx, List.of("Printed a " + type.name().toLowerCase(Locale.ROOT) + " chart from " + WeatherSources.get(level).name() + " weather."));
	}

	/** Makes {@code event} the next one, starting when the current one ends: processors forecast it as they would. */
	private static int forecastSolar(CommandContext<CommandSourceStack> ctx) {
		String name = StringArgumentType.getString(ctx, "event");
		com.arrl.radiocraft.common.radio.solar.SolarEvent event = RadiocraftData.SOLAR_EVENTS.getValue(Radiocraft.id(name));
		if(event == null)
			return send(ctx, List.of("no solar event " + name));
		SolarEventManager.forecast(ctx.getSource().getLevel(), event);
		SolarEventManager.Upcoming upcoming = SolarEventManager.getUpcoming(ctx.getSource().getLevel());
		return send(ctx, List.of("next event " + name + " in " + (upcoming.startGameTime() - ctx.getSource().getLevel().getGameTime()) + " ticks"));
	}

	private static int solar(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		SolarEventInstance event = SolarEventManager.getEvent(level);
		List<String> out = new ArrayList<>();
		out.add("dimension=" + level.dimension().location() + " dayTime=" + level.getDayTime() % 24000 + " isDay=" + level.isDay()
				+ " raining=" + level.isRaining() + " thundering=" + level.isThundering());
		if(event == null)
			out.add("solarEvent=none");
		else
			out.add(String.format(Locale.ROOT, "solarEvent=%s noise=%.3f progress=%d/%d",
					RadiocraftData.SOLAR_EVENTS.getKey(event.getEvent()), event.getEvent().getNoise(), event.getTicks(), event.getDuration()));
		if(event != null) {
			StringBuilder bands = new StringBuilder("bands (" + (level.isDay() ? "day" : "night") + "):");
			for(String band : List.of("10m", "20m", "40m", "80m"))
				bands.append(' ').append(band).append('=').append(event.getEvent().getCondition(band, level.isDay()).getSerializedName());
			out.add(bands.toString());
		}
		Aurora.Window aurora = Aurora.getWindow(level);
		long now = level.getGameTime();
		if(aurora == null || aurora.end() <= now)
			out.add("aurora=none");
		else if(aurora.start() > now)
			out.add("aurora=expected in " + (aurora.start() - now) + " ticks, lasts " + (aurora.end() - aurora.start()));
		else
			out.add("aurora=active for " + (aurora.end() - now) + " more ticks, past |Z| " + Aurora.latitude());
		SolarEventManager.Upcoming upcoming = SolarEventManager.getUpcoming(level);
		out.add(upcoming == null ? "next=not picked yet" : "next=" + RadiocraftData.SOLAR_EVENTS.getKey(upcoming.event()) + " in " + (upcoming.startGameTime() - now) + " ticks");
		return send(ctx, out);
	}

	/** Starts a fresh spell of the named event ("blackout") in this dimension, for playtesting the weather. */
	private static int setSolar(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		ResourceLocation id = Radiocraft.id(StringArgumentType.getString(ctx, "event"));
		if(!RadiocraftData.SOLAR_EVENTS.getKeys().contains(id)) {
			ctx.getSource().sendFailure(Component.literal("No solar event " + id));
			return 0;
		}
		SolarEventManager.startEvent(level, RadiocraftData.SOLAR_EVENTS.getValue(id));
		return solar(ctx);
	}

	/** Lights an aurora for a day right now, or puts it out (also cancels a forecast one). */
	private static int aurora(CommandContext<CommandSourceStack> ctx, boolean on) {
		ServerLevel level = ctx.getSource().getLevel();
		if(on)
			Aurora.schedule(level, level.getGameTime(), 24000);
		else
			Aurora.clear(level);
		return solar(ctx);
	}

	/** Opens (for a day) or closes sporadic E in this dimension. */
	private static int sporadicE(CommandContext<CommandSourceStack> ctx, boolean on) {
		ServerLevel level = ctx.getSource().getLevel();
		if(on)
			com.arrl.radiocraft.common.radio.solar.SporadicE.open(level, 24000);
		else
			com.arrl.radiocraft.common.radio.solar.SporadicE.close(level);
		return send(ctx, List.of("sporadic E " + (on ? "open for a day" : "closed")));
	}

	/** Terrain map size and what terrain has cost the voice thread since the last reset. */
	private static int terrain(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		List<String> out = new ArrayList<>();
		TerrainAtlas atlas = TerrainPropagation.openAtlas(level);
		out.add("terrain_enabled=" + TerrainPropagation.enabled() + (atlas == null ? " (no terrain map in this dimension)" : String.format(Locale.ROOT,
				" map: %d chunks, about %.1f MB", atlas.chunkCount(), atlas.estimatedBytes() / 1048576.0D)));
		long calls = TerrainPropagation.CALLS.sum();
		long nanos = TerrainPropagation.CALL_NANOS.sum();
		out.add(String.format(Locale.ROOT, "since reset: %d signal paths checked, %.2f us each on average, %.1f ms in all; worked out %d paths and %d horizons (the rest came from the cache)",
				calls, calls == 0 ? 0.0D : nanos / 1e3D / calls, nanos / 1e6D, TerrainPropagation.PATHS_WORKED_OUT.sum(), TerrainPropagation.HORIZONS_WORKED_OUT.sum()));
		return send(ctx, out);
	}

	private static int terrainReset(CommandContext<CommandSourceStack> ctx) {
		TerrainPropagation.resetCounters();
		TerrainPropagation.clearCaches();
		return send(ctx, List.of("terrain counters reset and caches cleared"));
	}

	/** What terrain does to a signal between two points on a band: line-of-sight loss and each end's skip clearance. */
	private static int terrainPath(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerLevel level = ctx.getSource().getLevel();
		BlockPos from = BlockPosArgument.getBlockPos(ctx, "from");
		BlockPos to = BlockPosArgument.getBlockPos(ctx, "to");
		Band band = Band.getBand(StringArgumentType.getString(ctx, "band"));
		TerrainAtlas atlas = TerrainPropagation.openAtlas(level);
		if(band == null)
			return fail(ctx, "No such band");
		if(atlas == null)
			return fail(ctx, "Terrain is off here (terrain_enabled, or a dimension with a ceiling)");
		TerrainPropagation.clearCaches();
		double db = TerrainPropagation.pathLossDb(level, atlas, band, from, to);
		List<String> out = new ArrayList<>();
		out.add(String.format(Locale.ROOT, "%s %s -> %s, %.0f blocks: line of sight loses %.1f dB (x%.3f)", band.name(),
				from.toShortString(), to.toShortString(), Math.sqrt(from.distSqr(to)), db, TerrainMath.dbToFactor(db)));
		out.add(String.format(Locale.ROOT, "skip (takeoff %.0f deg): x%.3f", TerrainPropagation.takeoffDegrees(band),
				TerrainPropagation.factor(level, band, from, false, to, false, true)));
		float[] horizon = TerrainPropagation.horizon(level, atlas, from);
		StringBuilder line = new StringBuilder("horizon at from (deg, from +Z clockwise to +X):");
		for(float angle : horizon)
			line.append(' ').append(Math.round(Math.toDegrees(angle)));
		out.add(line.toString());
		return send(ctx, out);
	}

	/** The server's tapes, and where they are kept. */
	private static int tapes(CommandContext<CommandSourceStack> ctx) {
		var server = ctx.getSource().getServer();
		List<String> out = new ArrayList<>();
		out.add("tape folder: " + com.arrl.radiocraft.common.radio.tape.TapeLibrary.folder(server));
		List<String> tracks = new ArrayList<>(com.arrl.radiocraft.common.radio.tape.TapeLibrary.tracks(server, ""));
		tracks.addAll(com.arrl.radiocraft.common.radio.tape.TapeLibrary.tracks(server, com.arrl.radiocraft.common.radio.tape.TapeLibrary.NUMBERS));
		out.add(tracks.isEmpty() ? "no .ogg files (numbers stations read out the ones in numbers/)" : String.join(", ", tracks));
		return send(ctx, out);
	}

	/** Gives you a tape as found in a structure, with {@code track} on it (a path in the tape folder) in a random colourway. */
	private static int giveTape(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		var player = ctx.getSource().getPlayerOrException();
		String track = StringArgumentType.getString(ctx, "track");
		net.minecraft.world.item.ItemStack tape = new net.minecraft.world.item.ItemStack(com.arrl.radiocraft.common.init.RadiocraftItems.TAPE_REEL.get());
		tape.set(com.arrl.radiocraft.common.init.RadiocraftDataComponent.TAPE_CONTENTS.get(), new com.arrl.radiocraft.common.datacomponents.TapeContents(
				player.getRandom().nextInt(com.arrl.radiocraft.common.datacomponents.TapeContents.COLORWAYS), track, "", 0));
		if(!player.getInventory().add(tape))
			player.drop(tape, false);
		return send(ctx, List.of("gave a tape of " + track));
	}

	/** A commercial radio module's program and frequency. */
	private static int module(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
		if(!(ctx.getSource().getLevel().getBlockEntity(pos) instanceof com.arrl.radiocraft.common.blockentities.CommercialRadioModuleBlockEntity module))
			return fail(ctx, "No commercial radio module (its bottom cell) at " + pos.toShortString());
		return send(ctx, List.of(String.format(java.util.Locale.ROOT, "module at %s: %s on %s, %.4f MHz", pos.toShortString(),
				module.getKind() == null ? "not started" : module.getKind().name().toLowerCase(java.util.Locale.ROOT), module.getBandName(), module.getFrequency() / 1e6)));
	}

	/** The commercial radio module stations on the air in this level, nearest first (predicted ones are at sites not generated yet). */
	private static int stations(CommandContext<CommandSourceStack> ctx) {
		net.minecraft.server.level.ServerLevel level = ctx.getSource().getLevel();
		BlockPos from = BlockPos.containing(ctx.getSource().getPosition());
		List<com.arrl.radiocraft.common.radio.CommercialStations.Station> stations = new java.util.ArrayList<>(
				com.arrl.radiocraft.common.radio.CommercialStations.get(level).stations());
		stations.sort(java.util.Comparator.comparingDouble(station -> station.pos().distSqr(from)));
		List<String> lines = new java.util.ArrayList<>();
		lines.add(stations.size() + " commercial stations on the air");
		for(com.arrl.radiocraft.common.radio.CommercialStations.Station station : stations.subList(0, Math.min(12, stations.size())))
			lines.add(String.format(java.util.Locale.ROOT, "%s (%d blocks): %s %s, %.4f MHz, %s%s", station.pos().toShortString(),
					(int)Math.sqrt(station.pos().distSqr(from)), station.kind().name().toLowerCase(java.util.Locale.ROOT), station.band(),
					station.frequency() / 1e6, station.callsign(), station.predicted() ? " (predicted)" : ""));
		return send(ctx, lines);
	}

	/** Sets a commercial radio module's program (numbers, static_beacon, dead_air, df_beeper, cw_beacon) and frequency. */
	private static int setModule(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
		if(!(ctx.getSource().getLevel().getBlockEntity(pos) instanceof com.arrl.radiocraft.common.blockentities.CommercialRadioModuleBlockEntity module))
			return fail(ctx, "No commercial radio module (its bottom cell) at " + pos.toShortString());
		float hz = (float)(DoubleArgumentType.getDouble(ctx, "mhz") * 1e6);
		com.arrl.radiocraft.common.radio.Band band = com.arrl.radiocraft.common.radio.Band.getBand(hz);
		if(band == null)
			return fail(ctx, "That frequency is in no band");
		module.configure(com.arrl.radiocraft.common.radio.voice.StationProgram.Kind.byName(StringArgumentType.getString(ctx, "program")), band, hz);
		return module(ctx);
	}

	private static int send(CommandContext<CommandSourceStack> ctx, List<String> lines) {
		ctx.getSource().sendSuccess(() -> Component.literal(String.join("\n", lines)), false);
		return 1;
	}

	private static int fail(CommandContext<CommandSourceStack> ctx, String message) {
		ctx.getSource().sendFailure(Component.literal(message));
		return 0;
	}
}
