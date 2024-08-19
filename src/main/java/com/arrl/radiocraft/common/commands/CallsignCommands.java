package com.arrl.radiocraft.common.commands;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

import java.util.Collection;

public class CallsignCommands {

	/*

	TODO: Refactor this to not use the capabilities system for this

	I don't know why we would use a capability on an entity to track the user's callsign.
	What would make more sense is if we correlated callsigns to player names and stored them in the save file.
	Doing this with capabilities is more designed around being able to set the capability on a block/item eg. a radio.



	public static final LiteralArgumentBuilder<CommandSourceStack> BUILDER =
			Commands.literal("callsign").executes(command -> getCallsign(command.getSource(), null))

					.then(Commands.argument("player", GameProfileArgument.gameProfile())
							.executes(command -> getCallsign(command.getSource(), GameProfileArgument.getGameProfiles(command, "player"))))

					.then(Commands.literal("reset")
							.then(Commands.argument("player", GameProfileArgument.gameProfile())
									.executes(command -> resetCallsign(command.getSource(), GameProfileArgument.getGameProfiles(command, "player")))))

					.then(Commands.literal("set")
							.then(Commands.argument("player", GameProfileArgument.gameProfile())
							.then(Commands.argument("callsign", StringArgumentType.string())
									.executes(command -> setCallsign(command.getSource(), GameProfileArgument.getGameProfiles(command, "player"), StringArgumentType.getString(command, "callsign"))))));



	public static int getCallsign(CommandSourceStack source, Collection<GameProfile> gameProfiles) throws CommandSyntaxException {
		if(gameProfiles != null && gameProfiles.size() > 1) {
			source.sendFailure(Component.translatable(Radiocraft.translationKey("commands", "callsign.get.failure.multiple")));
			return 0;
		}

		final GameProfile targetProfile = getTarget(source, gameProfiles);

		source.getLevel().getServer().overworld().getCapability(RadiocraftCapabilities.CALLSIGNS).ifPresent(cap -> {
			String callsign = cap.getCallsign(targetProfile.getId());
			if(callsign != null)
				source.sendSuccess(Component.translatable(Radiocraft.translationKey("commands", "callsign.get.success"), ComponentUtils.getDisplayName(targetProfile), callsign), true);
			else
				source.sendSuccess(Component.translatable(Radiocraft.translationKey("commands", "callsign.get.failure"), ComponentUtils.getDisplayName(targetProfile)), true);
		});

		return 1;
	}

	public static int resetCallsign(CommandSourceStack source, Collection<GameProfile> gameProfiles) throws CommandSyntaxException {
		if(gameProfiles.size() > 1) {
			source.sendFailure(Component.translatable(Radiocraft.translationKey("commands", "callsign.reset.failure.multiple")));
			return 0;
		}

		final GameProfile targetProfile = getTarget(source, gameProfiles);
		source.getLevel().getServer().overworld().getCapability(RadiocraftCapabilities.CALLSIGNS).ifPresent(cap -> {
			cap.resetCallsign(targetProfile.getId());
			source.sendSuccess(Component.translatable(Radiocraft.translationKey("commands", "callsign.reset.success"), ComponentUtils.getDisplayName(targetProfile)), true);
		});

		return 1;
	}

	public static int setCallsign(CommandSourceStack source, Collection<GameProfile> gameProfiles, String callsign) throws CommandSyntaxException {
		if(gameProfiles != null && gameProfiles.size() > 1) {
			source.sendFailure(Component.translatable(Radiocraft.translationKey("commands", "callsign.set.failure.multiple")));
			return 0;
		}

		final GameProfile targetProfile = getTarget(source, gameProfiles);
		source.getLevel().getServer().overworld().getCapability(RadiocraftCapabilities.CALLSIGNS).ifPresent(cap -> {
			cap.setCallsign(targetProfile.getId(), callsign);
			source.sendSuccess(Component.translatable(Radiocraft.translationKey("commands", "callsign.set.success"), ComponentUtils.getDisplayName(targetProfile), callsign), true);
		});

		return 1;
	}

	public static GameProfile getTarget(CommandSourceStack source, Collection<GameProfile> gameProfiles) throws CommandSyntaxException {
		return gameProfiles == null || gameProfiles.isEmpty() ?
				source.getPlayerOrException().getGameProfile() :
				gameProfiles.stream().findFirst().get();
	}
	 */

}
