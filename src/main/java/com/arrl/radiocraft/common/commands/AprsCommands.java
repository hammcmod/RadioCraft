package com.arrl.radiocraft.common.commands;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.AprsTerminalBlockEntity;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Admin APRS commands, for operators and command blocks. Both reach every loaded digital interface with a radio on the
 * frequency ("all" for every one), regardless of range, and neither is digipeated.
 * <ul>
 *     <li>{@code /aprs beacon <frequency|all> <callsign> <comment>} beacons the command's position under that callsign.</li>
 *     <li>{@code /aprs broadcast <frequency|all> <callsign|all> <message>} sends a message to a callsign, or to everyone.</li>
 * </ul>
 * Frequencies are in MHz, such as 144.390.
 */
public class AprsCommands {

	private static final SimpleCommandExceptionType BAD_FREQUENCY = new SimpleCommandExceptionType(
			Component.translatable(Radiocraft.translationKey("commands", "aprs.bad_frequency")));

	public static final LiteralArgumentBuilder<CommandSourceStack> BUILDER = Commands.literal("aprs")
			.requires(source -> source.hasPermission(2))
			.then(Commands.literal("beacon")
					.then(Commands.argument("frequency", StringArgumentType.word())
							.then(Commands.argument("callsign", StringArgumentType.word())
									.then(Commands.argument("comment", StringArgumentType.greedyString())
											.executes(AprsCommands::beacon)))))
			.then(Commands.literal("broadcast")
					.then(Commands.argument("frequency", StringArgumentType.word())
							.then(Commands.argument("target", StringArgumentType.word())
									.then(Commands.argument("message", StringArgumentType.greedyString())
											.executes(AprsCommands::broadcast)))));

	private static int beacon(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		float hz = parseFrequency(StringArgumentType.getString(context, "frequency"));
		TncPacket packet = TncPacket.beacon(source.getLevel().getRandom().nextLong(), StringArgumentType.getString(context, "callsign"),
				BlockPos.containing(source.getPosition()), StringArgumentType.getString(context, "comment"), "TOWER");
		return deliver(source, packet, hz);
	}

	private static int broadcast(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		float hz = parseFrequency(StringArgumentType.getString(context, "frequency"));
		String target = StringArgumentType.getString(context, "target");
		TncPacket packet = TncPacket.message(source.getLevel().getRandom().nextLong(), "SERVER",
				target.equalsIgnoreCase("all") ? TncPacket.EVERYONE : target, StringArgumentType.getString(context, "message"));
		return deliver(source, packet, hz);
	}

	/** Puts the packet into every loaded APRS terminal listening on {@code hz} (any, if NaN) and reports how many. */
	private static int deliver(CommandSourceStack source, TncPacket packet, float hz) {
		int reached = 0;
		for(AprsTerminalBlockEntity terminal : AprsTerminalBlockEntity.loaded()) {
			if(Float.isNaN(hz) || terminal.isListeningOn(hz)) {
				terminal.receiveDirect(packet);
				reached++;
			}
		}
		int count = reached;
		source.sendSuccess(() -> Component.translatable(Radiocraft.translationKey("commands", "aprs.sent"), count), true);
		return count;
	}

	/** MHz to Hz, or NaN for "all". */
	private static float parseFrequency(String text) throws CommandSyntaxException {
		if(text.toLowerCase(Locale.ROOT).equals("all"))
			return Float.NaN;
		double mhz;
		try {
			mhz = Double.parseDouble(text);
		}
		catch(NumberFormatException e) {
			throw BAD_FREQUENCY.create();
		}
		if(mhz <= 0.0D)
			throw BAD_FREQUENCY.create();
		return (float)(mhz * 1_000_000.0D);
	}
}
