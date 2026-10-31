package com.arrl.radiocraft.common.commands;

import com.arrl.radiocraft.common.init.RadiocraftData;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * /solarweather: the solar weather in this dimension (the event, how far into it, each band's condition, the aurora
 * and the next event). Operators can change it for testing:
 * <ul>
 *     <li>/solarweather set &lt;event&gt; - start that event now (blackout, poor, fair, average, good, great)</li>
 *     <li>/solarweather forecast &lt;event&gt; - make it the next event, as Ionospheric Processors would forecast it</li>
 *     <li>/solarweather aurora on|off and /solarweather sporadic_e on|off</li>
 * </ul>
 * The same as /rcdebug solar.
 */
public class SolarWeatherCommands {

	private static final SuggestionProvider<CommandSourceStack> EVENTS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(RadiocraftData.SOLAR_EVENTS.getKeys().stream().map(ResourceLocation::getPath), builder);

	public static final LiteralArgumentBuilder<CommandSourceStack> BUILDER = Commands.literal("solarweather")
			.executes(DebugCommands::solar)
			.then(Commands.literal("set").requires(source -> source.hasPermission(2))
					.then(Commands.argument("event", StringArgumentType.word()).suggests(EVENTS).executes(DebugCommands::setSolar)))
			.then(Commands.literal("forecast").requires(source -> source.hasPermission(2))
					.then(Commands.argument("event", StringArgumentType.word()).suggests(EVENTS).executes(DebugCommands::forecastSolar)))
			.then(Commands.literal("aurora").requires(source -> source.hasPermission(2))
					.then(Commands.literal("on").executes(ctx -> DebugCommands.aurora(ctx, true)))
					.then(Commands.literal("off").executes(ctx -> DebugCommands.aurora(ctx, false))))
			.then(Commands.literal("sporadic_e").requires(source -> source.hasPermission(2))
					.then(Commands.literal("on").executes(ctx -> DebugCommands.sporadicE(ctx, true)))
					.then(Commands.literal("off").executes(ctx -> DebugCommands.sporadicE(ctx, false))));
}
