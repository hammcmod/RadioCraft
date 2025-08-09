package com.arrl.radiocraft.common.commands;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.LicenseClass;
import com.arrl.radiocraft.api.capabilities.PlayerCallsignData;
import com.arrl.radiocraft.common.data.PlayerCallsignSavedData;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.command.EnumArgument;

import java.util.Collection;
import java.util.function.Supplier;

public class CallsignCommands {

	private static PlayerCallsignSavedData savedData;

	private static PlayerCallsignSavedData getPlayerCallsignSavedData(Level level) {
		if (savedData == null) {
			MinecraftServer server = level.getServer();
			if (server != null) {
				savedData = PlayerCallsignSavedData.get(level.getServer().overworld());
			} else {
				throw new IllegalStateException("Cannot get PlayerCallsignSavedData if the server is null");
			}
		}
		return savedData;
	}

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
									.then(Commands.argument("license", EnumArgument.enumArgument(LicenseClass.class))
									.executes(command -> setCallsign(command.getSource(), GameProfileArgument.getGameProfiles(command, "player"), StringArgumentType.getString(command, "callsign"), command.getArgument("license", LicenseClass.class)))))));



	public static int getCallsign(CommandSourceStack source, Collection<GameProfile> gameProfiles) throws CommandSyntaxException {
		if(gameProfiles != null && gameProfiles.size() > 1) {
			source.sendFailure(Component.translatable(Radiocraft.translationKey("commands", "callsign.get.failure.multiple")));
			return 0;
		}
		final GameProfile targetProfile = getTarget(source, gameProfiles);
		savedData = getPlayerCallsignSavedData(source.getLevel());
		PlayerCallsignData data = savedData.getCallsignData(targetProfile.getId());
		Component name = Component.literal(targetProfile.getName());
		if(data != null) {
			Component callsign = Component.literal(data.callsign());
			Component license = Component.translatable(Radiocraft.translationKey("license_class", data.licenseClass().name()));
			Supplier<Component> combined = () -> Component.translatable(Radiocraft.translationKey("commands", "callsign.get.success"), name, callsign, license);
			source.sendSuccess(combined, true);
		} else {
			Supplier<Component> combined = () -> Component.translatable(Radiocraft.translationKey("commands", "callsign.get.failure"), name);
			source.sendSuccess(combined, true);
		}
		return 1;
	}

	public static int resetCallsign(CommandSourceStack source, Collection<GameProfile> gameProfiles) throws CommandSyntaxException {
		if(gameProfiles.size() > 1) {
			source.sendFailure(Component.translatable(Radiocraft.translationKey("commands", "callsign.reset.failure.multiple")));
			return 0;
		}
		final GameProfile targetProfile = getTarget(source, gameProfiles);
		savedData = getPlayerCallsignSavedData(source.getLevel());
		savedData.resetCallsign(targetProfile.getId());
		Component name = Component.literal(targetProfile.getName());
		Supplier<Component> combined = () -> Component.translatable(Radiocraft.translationKey("commands", "callsign.reset.success"), name);
		source.sendSuccess(combined, true);
		return 1;
	}

	public static int setCallsign(CommandSourceStack source, Collection<GameProfile> gameProfiles, String callsign, LicenseClass licenseClass) throws CommandSyntaxException {
		if(gameProfiles != null && gameProfiles.size() > 1) {
			source.sendFailure(Component.translatable(Radiocraft.translationKey("commands", "callsign.set.failure.multiple")));
			return 0;
		}
		final GameProfile targetProfile = getTarget(source, gameProfiles);
		savedData = getPlayerCallsignSavedData(source.getLevel());
		savedData.setCallsignData(targetProfile.getId(), new PlayerCallsignData(targetProfile.getId().toString(), callsign, licenseClass));
		Component name = Component.literal(targetProfile.getName());
		Component license = Component.translatable(Radiocraft.translationKey("license_class", licenseClass.name()));
		Supplier<Component> combined = () -> Component.translatable(Radiocraft.translationKey("commands", "callsign.set.success"), name, callsign, license);
		source.sendSuccess(combined, true);
		return 1;
	}

	public static GameProfile getTarget(CommandSourceStack source, Collection<GameProfile> gameProfiles) throws CommandSyntaxException {
		return gameProfiles == null || gameProfiles.isEmpty() ?
				source.getPlayerOrException().getGameProfile() :
				gameProfiles.stream().findFirst().get();
	}
}
