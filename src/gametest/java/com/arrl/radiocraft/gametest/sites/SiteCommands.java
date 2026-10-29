package com.arrl.radiocraft.gametest.sites;

import com.arrl.radiocraft.Radiocraft;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Map;
import java.util.Set;

/**
 * {@code /rcsite}, dev runs only, for making the radio sites:
 * <ul>
 * <li>{@code build <site> <pos>}: builds it (see {@link Sites}) with its corner at pos;</li>
 * <li>{@code save <site> <pos>}: saves what stands there as its template, in the mod's resources;</li>
 * <li>{@code capture <site> <from> <to> <powered>}: saves any box as a site's template (a site built by hand);</li>
 * <li>{@code place <site> <pos> [variants]}: puts the saved template down there as the world generator would (all
 * its optional parts, or with {@code variants} true the ones a site generated there would keep);</li>
 * <li>{@code clear <from> <to>}: air (and no block entities) through the box;</li>
 * <li>{@code check <from> <to> [powered]}: every station in the box, and what would leave it not working;</li>
 * <li>{@code predict <pos>}: puts the module stations of the sites due in the grid cells holding pos on the air;</li>
 * <li>{@code survey <cells>}: which sites the generator would put in the grid cells round you, and how long working
 * that out takes a cell.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public final class SiteCommands {

	private SiteCommands() {}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("rcsite").requires(source -> source.hasPermission(2))
				.then(Commands.literal("build").then(site().then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(SiteCommands::build))))
				.then(Commands.literal("save").then(site().then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(SiteCommands::save))))
				.then(Commands.literal("capture").then(Commands.argument("site", StringArgumentType.word())
						.then(Commands.argument("from", BlockPosArgument.blockPos()).then(Commands.argument("to", BlockPosArgument.blockPos())
								.then(Commands.argument("powered", BoolArgumentType.bool()).executes(SiteCommands::capture))))))
				.then(Commands.literal("place").then(Commands.argument("site", StringArgumentType.word())
						.then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx -> place(ctx, false))
								.then(Commands.argument("variants", BoolArgumentType.bool()).executes(ctx -> place(ctx, BoolArgumentType.getBool(ctx, "variants")))))))
				.then(Commands.literal("clear").then(Commands.argument("from", BlockPosArgument.blockPos())
						.then(Commands.argument("to", BlockPosArgument.blockPos()).executes(SiteCommands::clear))))
				.then(Commands.literal("survey").then(Commands.argument("cells", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 64))
						.executes(SiteCommands::survey)))
				.then(Commands.literal("predict").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(SiteCommands::predict)))
				.then(Commands.literal("check").then(Commands.argument("from", BlockPosArgument.blockPos())
						.then(Commands.argument("to", BlockPosArgument.blockPos()).executes(ctx -> check(ctx, true))
								.then(Commands.argument("powered", BoolArgumentType.bool()).executes(ctx -> check(ctx, BoolArgumentType.getBool(ctx, "powered"))))))));
	}

	private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> site() {
		return Commands.argument("site", StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Sites.ALL.keySet(), builder));
	}

	private static Sites.Site siteOf(CommandContext<CommandSourceStack> ctx) {
		Sites.Site site = Sites.ALL.get(StringArgumentType.getString(ctx, "site"));
		if(site == null)
			throw new IllegalArgumentException("No site " + StringArgumentType.getString(ctx, "site") + ": " + Sites.ALL.keySet());
		return site;
	}

	private static void say(CommandContext<CommandSourceStack> ctx, String text) {
		ctx.getSource().sendSuccess(() -> Component.literal(text), false);
	}

	private static int build(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		Sites.Site site = siteOf(ctx);
		BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
		ServerLevel level = ctx.getSource().getLevel();
		clear(level, pos, pos.offset(site.sizeX() - 1, site.sizeY() - 1, site.sizeZ() - 1));
		site.build().accept(new SiteBuilder(level, pos.offset(site.margin(), 0, site.margin())));
		say(ctx, "Built " + site.name() + " from " + pos.toShortString() + " to " + pos.offset(site.sizeX() - 1, site.sizeY() - 1, site.sizeZ() - 1).toShortString());
		return 1;
	}

	private static int save(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		Sites.Site site = siteOf(ctx);
		return save(ctx, site.name(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"), new Vec3i(site.sizeX(), site.sizeY(), site.sizeZ()), site.powered());
	}

	private static int capture(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		BlockPos from = BlockPosArgument.getLoadedBlockPos(ctx, "from"), to = BlockPosArgument.getLoadedBlockPos(ctx, "to");
		BlockPos min = BlockPos.min(from, to), max = BlockPos.max(from, to);
		return save(ctx, StringArgumentType.getString(ctx, "site"), min, new Vec3i(max.getX() - min.getX() + 1, max.getY() - min.getY() + 1,
				max.getZ() - min.getZ() + 1), BoolArgumentType.getBool(ctx, "powered"));
	}

	private static int save(CommandContext<CommandSourceStack> ctx, String name, BlockPos from, Vec3i size, boolean powered) {
		try {
			Map<String, Set<String>> kept = SiteTemplates.save(ctx.getSource().getLevel(), from, size, name, powered);
			say(ctx, "Saved " + name + " (" + size.getX() + "x" + size.getY() + "x" + size.getZ() + ") to " + SiteTemplates.file(name).toAbsolutePath().normalize());
			kept.forEach((id, keys) -> say(ctx, "  " + id + ": " + keys));
			return 1;
		}
		catch(Exception e) {
			ctx.getSource().sendFailure(Component.literal("Could not save " + name + ": " + e));
			return 0;
		}
	}

	private static int place(CommandContext<CommandSourceStack> ctx, boolean variants) throws CommandSyntaxException {
		String name = StringArgumentType.getString(ctx, "site");
		BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
		try {
			StructureTemplate template = SiteTemplates.load(ctx.getSource().getLevel(), name);
			if(template == null) {
				ctx.getSource().sendFailure(Component.literal("No saved site " + name));
				return 0;
			}
			Vec3i size = template.getSize();
			clear(ctx.getSource().getLevel(), pos, pos.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1));
			SiteTemplates.place(ctx.getSource().getLevel(), name, pos, variants);
			say(ctx, "Placed " + name + " from " + pos.toShortString() + " to " + pos.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1).toShortString());
			return 1;
		}
		catch(Exception e) {
			ctx.getSource().sendFailure(Component.literal("Could not place " + name + ": " + e));
			return 0;
		}
	}

	private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		clear(ctx.getSource().getLevel(), BlockPosArgument.getLoadedBlockPos(ctx, "from"), BlockPosArgument.getLoadedBlockPos(ctx, "to"));
		say(ctx, "Cleared");
		return 1;
	}

	/** Air through the box, top down so nothing is left hanging to drop. */
	private static void clear(ServerLevel level, BlockPos from, BlockPos to) {
		BlockPos min = BlockPos.min(from, to), max = BlockPos.max(from, to);
		for(int y = max.getY(); y >= min.getY(); y--)
			for(BlockPos pos : BlockPos.betweenClosed(new BlockPos(min.getX(), y, min.getZ()), new BlockPos(max.getX(), y, max.getZ())))
				if(!level.getBlockState(pos).isAir())
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
	}

	/**
	 * Which site the world generator would put in each cell of the radio site grid within {@code cells} cells of the
	 * source (worked out, nothing generated), how many of each, and how long it took a cell; each grid in turn.
	 */
	private static int survey(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		java.util.List<com.arrl.radiocraft.common.worldgen.SitePrediction> predictions = com.arrl.radiocraft.common.worldgen.SitePrediction.of(level);
		if(predictions.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("No radio sites in this level"));
			return 0;
		}
		int all = 0;
		for(com.arrl.radiocraft.common.worldgen.SitePrediction prediction : predictions)
			all += survey(ctx, level, prediction);
		return all;
	}

	private static int survey(CommandContext<CommandSourceStack> ctx, ServerLevel level, com.arrl.radiocraft.common.worldgen.SitePrediction prediction) {
		int cells = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "cells"), cellBlocks = prediction.spacing() * 16;
		BlockPos from = BlockPos.containing(ctx.getSource().getPosition());
		int cx = Math.floorDiv(from.getX(), cellBlocks), cz = Math.floorDiv(from.getZ(), cellBlocks);
		Map<String, Integer> counts = new java.util.TreeMap<>();
		int modules = 0, total = 0;
		long start = System.nanoTime(), worst = 0;
		for(int dx = -cells; dx <= cells; dx++)
			for(int dz = -cells; dz <= cells; dz++) {
				long t = System.nanoTime();
				var site = prediction.site(cx + dx, cz + dz);
				modules += prediction.stations(cx + dx, cz + dz).size();
				worst = Math.max(worst, System.nanoTime() - t);
				total++;
				String name = site.map(found -> level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
						.getKey(found.getStructure()).getPath()).orElse("(none)");
				counts.merge(name, 1, Integer::sum);
			}
		long took = System.nanoTime() - start;
		say(ctx, prediction.key().location() + ", " + total + " cells of " + cellBlocks + " blocks: " + counts + ", " + modules + " module stations");
		say(ctx, String.format(java.util.Locale.ROOT, "%.2f ms a cell (twice over: the site, then its stations), worst %.2f ms", took / 1e6 / total, worst / 1e6));
		return total;
	}

	/** Puts the module stations of the sites the generator would put in the grid cells holding {@code pos} on the air now, as a player's coming near would. */
	private static int predict(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
		int found = 0;
		for(com.arrl.radiocraft.common.worldgen.SitePrediction prediction : com.arrl.radiocraft.common.worldgen.SitePrediction.of(level)) {
			int cellBlocks = prediction.spacing() * 16;
			for(com.arrl.radiocraft.common.radio.CommercialStations.Station station : prediction.stations(Math.floorDiv(pos.getX(), cellBlocks), Math.floorDiv(pos.getZ(), cellBlocks))) {
				if(com.arrl.radiocraft.common.radio.CommercialStations.get(level).get(station.pos()) == null)
					com.arrl.radiocraft.common.radio.CommercialStations.get(level).register(station);
				say(ctx, prediction.key().location() + ": " + station);
				found++;
			}
		}
		say(ctx, found + " stations");
		return found;
	}

	private static int check(CommandContext<CommandSourceStack> ctx, boolean powered) throws CommandSyntaxException {
		SiteCheck.Report report = SiteCheck.check(ctx.getSource().getLevel(), BlockPosArgument.getLoadedBlockPos(ctx, "from"),
				BlockPosArgument.getLoadedBlockPos(ctx, "to"), powered);
		report.lines().forEach(line -> say(ctx, line));
		say(ctx, report.problems().isEmpty() ? "No problems" : report.problems().size() + " problem(s)");
		return report.problems().size();
	}
}
