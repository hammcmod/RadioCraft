package com.arrl.radiocraft.common.commands;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.solar.Aurora;
import com.arrl.radiocraft.common.radio.solar.BandCondition;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * {@code /bandcalculate <from> <to>} (admin, from the proposal): which band carries between two points right now,
 * without a solar weather station. It is an estimate from the band ranges in the server config and the current solar
 * conditions; antennas, power and noise are not counted.
 */
public class BandCalculateCommands {

	public static final LiteralArgumentBuilder<CommandSourceStack> BUILDER =
			Commands.literal("bandcalculate").requires(source -> source.hasPermission(2))
					.then(Commands.argument("from", ColumnPosArgument.columnPos())
							.then(Commands.argument("to", ColumnPosArgument.columnPos())
									.executes(context -> run(context.getSource(), ColumnPosArgument.getColumnPos(context, "from"), ColumnPosArgument.getColumnPos(context, "to")))));

	/** How a band would carry between the two points: line of sight, skip (with its condition), aurora, or not at all. */
	public enum Path { LINE_OF_SIGHT, SKIP, AURORA, NONE }

	/**
	 * One band's verdict. {@code score} ranks the bands: line of sight 1.0, skip its condition's multiplier, aurora the
	 * aurora strength, none 0.
	 */
	public record Result(Band band, Path path, @Nullable BandCondition condition, double score) {}

	/** Every configured band between {@code a} and {@code b}, best first. */
	public static List<Result> calculate(Level level, BlockPos a, BlockPos b) {
		double distance = Math.sqrt(a.distToCenterSqr(b.getX() + 0.5D, a.getY() + 0.5D, b.getZ() + 0.5D));
		List<Result> results = new ArrayList<>();
		for(RadiocraftServerConfig.BandConfig config : RadiocraftServerConfig.BAND_CONFIGS.values()) {
			Band band = config.getBand();
			if(distance <= band.losRange()) {
				results.add(new Result(band, Path.LINE_OF_SIGHT, null, 1.0D));
				continue;
			}
			if(band.isVhf()) {
				boolean aurora = Aurora.isActive(level) && Aurora.inZone(a) && Aurora.inZone(b)
						&& distance >= RadiocraftServerConfig.AURORA_VHF_MIN_RANGE.get() && distance <= RadiocraftServerConfig.AURORA_VHF_MAX_RANGE.get();
				results.add(aurora ? new Result(band, Path.AURORA, null, RadiocraftServerConfig.AURORA_VHF_STRENGTH.get()) : new Result(band, Path.NONE, null, 0.0D));
				continue;
			}
			boolean day = level.isDay();
			int min = day ? band.minSkipDay() : band.minSkipNight(), max = day ? band.maxSkipDay() : band.maxSkipNight();
			BandCondition condition = SolarEventManager.getCondition(level, band, a, b);
			if(max > 0 && distance >= min && distance <= max && condition != BandCondition.CLOSED)
				results.add(new Result(band, Path.SKIP, condition, condition.getSkipMultiplier()));
			else
				results.add(new Result(band, Path.NONE, condition, 0.0D));
		}
		results.sort(Comparator.comparingDouble(Result::score).reversed());
		return results;
	}

	private static int run(CommandSourceStack source, ColumnPos from, ColumnPos to) {
		Level level = source.getLevel();
		BlockPos a = new BlockPos(from.x(), 0, from.z()), b = new BlockPos(to.x(), 0, to.z());
		List<Result> results = calculate(level, a, b);
		int distance = (int)Math.round(Math.sqrt(a.distSqr(b)));
		source.sendSuccess(() -> Component.literal("Bands from " + from.x() + ", " + from.z() + " to " + to.x() + ", " + to.z()
				+ " (" + distance + " blocks, " + (level.isDay() ? "day" : "night") + "):").withStyle(ChatFormatting.GOLD), false);
		for(Result result : results)
			source.sendSuccess(() -> Component.literal("  " + result.band().name() + ": " + describe(result))
					.withStyle(result.score() > 0.0D ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
		Result best = results.getFirst();
		source.sendSuccess(() -> best.score() > 0.0D
				? Component.literal("Best: " + best.band().name()).withStyle(ChatFormatting.AQUA)
				: Component.literal("No band carries this path right now").withStyle(ChatFormatting.RED), false);
		return best.score() > 0.0D ? 1 : 0;
	}

	private static String describe(Result result) {
		return switch(result.path()) {
			case LINE_OF_SIGHT -> "line of sight";
			case SKIP -> "skip, " + result.condition().getSerializedName();
			case AURORA -> "aurora skip";
			case NONE -> result.condition() == BandCondition.CLOSED ? "closed" : "out of range";
		};
	}
}
