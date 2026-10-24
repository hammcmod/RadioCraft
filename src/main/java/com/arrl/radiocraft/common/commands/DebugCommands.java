package com.arrl.radiocraft.common.commands;

import com.arrl.radiocraft.Radiocraft;
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
			.then(Commands.literal("solar").executes(DebugCommands::solar)
					.then(Commands.literal("set").then(Commands.argument("event", StringArgumentType.word())
							.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RadiocraftData.SOLAR_EVENTS.getKeys().stream().map(ResourceLocation::getPath), builder))
							.executes(DebugCommands::setSolar)))
					.then(Commands.literal("aurora")
							.then(Commands.literal("on").executes(ctx -> aurora(ctx, true)))
							.then(Commands.literal("off").executes(ctx -> aurora(ctx, false)))))
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
		if(IBENetworks.getObject(level, pos) instanceof AntennaNetworkObject obj)
			out.add("networkObject antenna=" + (obj.getAntenna() == null ? "none (not yet recalculated?)" : obj.getAntenna().type.getId()));
		return send(ctx, out);
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
		for(Direction dir : Direction.values()) {
			BENetwork network = obj.getNetwork(dir);
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

	/** Terrain map size and what terrain has cost the voice thread since the last reset. */
	private static int terrain(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		List<String> out = new ArrayList<>();
		TerrainAtlas atlas = TerrainPropagation.atlas(level);
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
		TerrainAtlas atlas = TerrainPropagation.atlas(level);
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

	private static int send(CommandContext<CommandSourceStack> ctx, List<String> lines) {
		ctx.getSource().sendSuccess(() -> Component.literal(String.join("\n", lines)), false);
		return 1;
	}

	private static int fail(CommandContext<CommandSourceStack> ctx, String message) {
		ctx.getSource().sendFailure(Component.literal(message));
		return 0;
	}
}
