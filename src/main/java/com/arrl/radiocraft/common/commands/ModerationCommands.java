package com.arrl.radiocraft.common.commands;

import com.arrl.radiocraft.common.radio.voice.RadioModeration;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /radio moderateall}, {@code /radio moderate <player>} and {@code /radio moderate off} (also under
 * {@code /radiocraft}), for operators: listen to radio traffic clean, with who is talking shown (see
 * {@link RadioModeration}).
 */
public class ModerationCommands {

	public static final LiteralArgumentBuilder<CommandSourceStack> BUILDER = root("radiocraft");
	public static final LiteralArgumentBuilder<CommandSourceStack> SHORT_BUILDER = root("radio");

	private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
		return Commands.literal(name)
				.then(Commands.literal("moderateall").requires(source -> source.hasPermission(2)).executes(ModerationCommands::all))
				.then(Commands.literal("moderate").requires(source -> source.hasPermission(2))
						.then(Commands.literal("off").executes(ModerationCommands::off))
						.then(Commands.argument("player", EntityArgument.player()).executes(ModerationCommands::one)));
	}

	private static int all(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer moderator = ctx.getSource().getPlayerOrException();
		RadioModeration.listen(moderator, RadioModeration.EVERYONE);
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.radiocraft.moderate.all"), true);
		return 1;
	}

	private static int one(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer moderator = ctx.getSource().getPlayerOrException();
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
		RadioModeration.listen(moderator, target.getUUID());
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.radiocraft.moderate.one", target.getDisplayName()), true);
		return 1;
	}

	private static int off(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer moderator = ctx.getSource().getPlayerOrException();
		boolean was = RadioModeration.stop(moderator.getUUID());
		ctx.getSource().sendSuccess(() -> Component.translatable(was ? "commands.radiocraft.moderate.off" : "commands.radiocraft.moderate.not_on"), true);
		return was ? 1 : 0;
	}
}
