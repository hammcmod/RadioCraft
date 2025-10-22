package com.arrl.radiocraft.common.commands;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.capabilities.BlockEntityCallsignData;
import com.arrl.radiocraft.api.capabilities.LicenseClass;
import com.arrl.radiocraft.api.capabilities.PlayerCallsignData;
import com.arrl.radiocraft.common.data.BlockEntityCallsignSavedData;
import com.arrl.radiocraft.common.data.PlayerCallsignSavedData;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.command.EnumArgument;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class CallsignCommands {

	private static PlayerCallsignSavedData playerSavedData;
	private static BlockEntityCallsignSavedData blockEntitySavedData;

	private static PlayerCallsignSavedData getPlayerCallsignSavedData(Level level) {
		if (playerSavedData == null) {
			MinecraftServer server = level.getServer();
			if (server != null) {
				playerSavedData = PlayerCallsignSavedData.get(level.getServer().overworld());
			} else {
				throw new IllegalStateException("Cannot get PlayerCallsignSavedData if the server is null");
			}
		}
		return playerSavedData;
	}

	private static BlockEntityCallsignSavedData getBlockEntityCallsignSavedData(Level level) {
		if (blockEntitySavedData == null) {
			MinecraftServer server = level.getServer();
			if (server != null) {
				blockEntitySavedData = BlockEntityCallsignSavedData.get(level.getServer().overworld());
			} else {
				throw new IllegalStateException("Cannot get BlockEntityCallsignSavedData if the server is null");
			}
		}
		return blockEntitySavedData;
	}

	public static final LiteralArgumentBuilder<CommandSourceStack> BUILDER =
			Commands.literal("callsign")
					.then(Commands.literal("list")
							.executes(command -> listCallsigns(command.getSource())))

					.then(Commands.literal("search")
							.then(Commands.argument("callsign", StringArgumentType.string())
									.executes(command -> getCallsign(command.getSource(), StringArgumentType.getString(command, "callsign")))))

					.then(Commands.literal("reset")
							.then(Commands.argument("player", GameProfileArgument.gameProfile())
									.executes(command -> resetCallsign(command.getSource(), GameProfileArgument.getGameProfiles(command, "player")))))

				.then(Commands.literal("generate")
					.requires(source -> source.hasPermission(3))
					.then(Commands.argument("player", GameProfileArgument.gameProfile())
							.executes(command -> generateCallsign(command.getSource(), GameProfileArgument.getGameProfiles(command, "player"), LicenseClass.TECHNICIAN))
							.then(Commands.argument("license", EnumArgument.enumArgument(LicenseClass.class))
								.executes(command -> generateCallsign(command.getSource(), GameProfileArgument.getGameProfiles(command, "player"), command.getArgument("license", LicenseClass.class))))))

				.then(Commands.literal("generate-online")
					.requires(source -> source.hasPermission(3))
					.executes(command -> generateCallsignForOnlinePlayers(command.getSource(), LicenseClass.TECHNICIAN))
					.then(Commands.argument("license", EnumArgument.enumArgument(LicenseClass.class))
						.executes(command -> generateCallsignForOnlinePlayers(command.getSource(), command.getArgument("license", LicenseClass.class)))))

				.then(Commands.literal("set")
						.then(Commands.argument("player", GameProfileArgument.gameProfile())
								.then(Commands.argument("callsign", StringArgumentType.string())
											.then(Commands.argument("license", EnumArgument.enumArgument(LicenseClass.class))
													.executes(command -> setCallsign(command.getSource(), GameProfileArgument.getGameProfiles(command, "player"), StringArgumentType.getString(command, "callsign"), command.getArgument("license", LicenseClass.class)))))))

					.then(Commands.literal("get")
							.then(Commands.argument("player", GameProfileArgument.gameProfile())
									.executes(command -> getPlayerCallsign(command.getSource(), GameProfileArgument.getGameProfiles(command, "player")))));

	public static int listCallsigns(CommandSourceStack source) {
		playerSavedData = getPlayerCallsignSavedData(source.getLevel());
		blockEntitySavedData = getBlockEntityCallsignSavedData(source.getLevel());

		HashSet<String> callsigns = new HashSet<>(playerSavedData.getCallsigns());
		callsigns.addAll(blockEntitySavedData.getCallsigns());

		if(callsigns.isEmpty()) {
			Supplier<Component> output = () -> Component.translatable(Radiocraft.translationKey("commands", "callsign.list.empty"));
			source.sendSuccess(output, true);
			return 1;
		}

		Supplier<Component> output = () -> Component.literal(String.join(", ", callsigns));
		source.sendSuccess(output, true);
		return 1;
	}

	public static int getCallsign(CommandSourceStack source, String search) throws CommandSyntaxException {
		ServerPlayer result = source.getServer().getPlayerList().getPlayers().stream()
				.filter(player -> player.getGameProfile().getName().toLowerCase().contains(search.toLowerCase()))
				.findFirst().orElse(null);
		playerSavedData = getPlayerCallsignSavedData(source.getLevel());
		if(result != null) {
			PlayerCallsignData data = playerSavedData.getCallsignData(result.getGameProfile().getId());
			Component name = Component.literal(result.getGameProfile().getName());
			if(data != null) {
				Component callsign = Component.literal(data.callsign());
				Component license = Component.translatable(Radiocraft.translationKey("license_class", data.licenseClass().name()));
				Supplier<Component> combined = () -> Component.translatable(Radiocraft.translationKey("commands", "callsign.get.success"), name, callsign, license);
				source.sendSuccess(combined, true);
			}
			return 1;
		}
		if (playerSavedData.getCallsigns().contains(search)) {
			PlayerCallsignData data = playerSavedData.getCallsignData(search);
			if(data != null) {
				ServerPlayer player = source.getServer().getPlayerList().getPlayer(UUID.fromString(data.playerUUID()));
				Component name = Component.literal(data.playerUUID());
				if (data.playerName() != null && !data.playerName().isEmpty()) {
					name = Component.literal(data.playerName());
				}
				if (player != null) {
					// If the player is null, it's a player who isn't online. We know their UUID but can't get their username (trivially).
					name = Component.literal(player.getGameProfile().getName());
				}
				Component finalName = name; // Gah. It's because lambdas must take final variables.
				Component callsign = Component.literal(data.callsign());
				Component license = Component.translatable(Radiocraft.translationKey("license_class", data.licenseClass().name()));
				Supplier<Component> combined = () -> Component.translatable(Radiocraft.translationKey("commands", "callsign.get.success"), finalName, callsign, license);
				source.sendSuccess(combined, true);
				return 1;
			}
		}
		if (blockEntitySavedData.getCallsigns().contains(search)) {
			BlockEntityCallsignData data = blockEntitySavedData.getCallsignData(search);

			if(data != null) {
				Component name = Component.literal(data.pos().toString());
				Component callsign = Component.literal(data.callsign());
				Component license = Component.translatable(Radiocraft.translationKey("license_class", data.licenseClass().name()));
				Supplier<Component> combined = () -> Component.translatable(Radiocraft.translationKey("commands", "callsign.get.success"), name, callsign, license);
				source.sendSuccess(combined, true);
				return 1;
			}
		}

		Supplier<Component> combined = () -> Component.translatable(Radiocraft.translationKey("commands", "callsign.get.failure"), search);
		source.sendSuccess(combined, true);

		return 1;
	}

	public static int getPlayerCallsign(CommandSourceStack source, Collection<GameProfile> gameProfiles) throws CommandSyntaxException {
		if(gameProfiles != null && gameProfiles.size() > 1) {
			source.sendFailure(Component.translatable(Radiocraft.translationKey("commands", "callsign.get.failure.multiple")));
			return 0;
		}
		final GameProfile targetProfile = getTarget(source, gameProfiles);
		playerSavedData = getPlayerCallsignSavedData(source.getLevel());
		PlayerCallsignData data = playerSavedData.getCallsignData(targetProfile.getId());
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
        if (RadiocraftServerConfig.CALLSIGN_PERMISSIONS_ENABLED.get() && !isPrivileged(source.getPlayerOrException())) {
            source.sendFailure(Component.translatable(Radiocraft.translationKey("commands", "callsign.reset.failure.permission")));
            return 0;
        }
		final GameProfile targetProfile = getTarget(source, gameProfiles);
		playerSavedData = getPlayerCallsignSavedData(source.getLevel());
		playerSavedData.resetCallsign(targetProfile.getId());
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
        if (RadiocraftServerConfig.CALLSIGN_PERMISSIONS_ENABLED.get() && !isPrivileged(source.getPlayerOrException())) {
            source.sendFailure(Component.translatable(Radiocraft.translationKey("commands", "callsign.set.failure.permission")));
            return 0;
        }
		final GameProfile targetProfile = getTarget(source, gameProfiles);
		playerSavedData = getPlayerCallsignSavedData(source.getLevel());
		playerSavedData.setCallsignData(targetProfile.getId(), new PlayerCallsignData(targetProfile.getId().toString(), targetProfile.getName(), callsign, licenseClass));
		Component name = Component.literal(targetProfile.getName());
		Component license = Component.translatable(Radiocraft.translationKey("license_class", licenseClass.name()));
		Supplier<Component> combined = () -> Component.translatable(Radiocraft.translationKey("commands", "callsign.set.success"), name, callsign, license);
		source.sendSuccess(combined, true);
		return 1;
	}

	public static int generateCallsign(CommandSourceStack source, Collection<GameProfile> gameProfiles, LicenseClass licenseClass) throws CommandSyntaxException {
		if (gameProfiles != null && gameProfiles.size() > 1) {
			source.sendFailure(Component.translatable(Radiocraft.translationKey("commands", "callsign.generate.failure.multiple")));
			return 0;
		}
		final GameProfile targetProfile = getTarget(source, gameProfiles);
		playerSavedData = getPlayerCallsignSavedData(source.getLevel());
		blockEntitySavedData = getBlockEntityCallsignSavedData(source.getLevel());
		PlayerCallsignData existingData = playerSavedData.getCallsignData(targetProfile.getId());
		if (existingData != null) {
			sendExistingCallsignMessage(source, targetProfile.getName(), existingData);
			return 1;
		}
		String generatedCallsign = generateCallsignFromUuid(targetProfile.getId());
		if (handleCallsignCollision(source, targetProfile.getId(), targetProfile.getName(), generatedCallsign)) {
			return 0;
		}
		PlayerCallsignData newData = new PlayerCallsignData(targetProfile.getId().toString(), targetProfile.getName(), generatedCallsign, licenseClass);
		playerSavedData.setCallsignData(targetProfile.getId(), newData);
		sendGeneratedCallsignMessage(source, targetProfile.getName(), generatedCallsign, licenseClass);
		return 1;
	}

	public static int generateCallsignForOnlinePlayers(CommandSourceStack source, LicenseClass licenseClass) {
		playerSavedData = getPlayerCallsignSavedData(source.getLevel());
		blockEntitySavedData = getBlockEntityCallsignSavedData(source.getLevel());
		List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
		if (players.isEmpty()) {
			source.sendSuccess(() -> Component.translatable(Radiocraft.translationKey("commands", "callsign.generate.online.empty")), true);
			return 0;
		}

		int generatedCount = 0;
		for (ServerPlayer player : players) {
			PlayerCallsignData existingData = playerSavedData.getCallsignData(player.getUUID());
			if (existingData != null) {
				sendExistingCallsignMessage(source, player.getGameProfile().getName(), existingData);
				continue;
			}
			String generatedCallsign = generateCallsignFromUuid(player.getUUID());
			if (handleCallsignCollision(source, player.getUUID(), player.getGameProfile().getName(), generatedCallsign)) {
				continue;
			}
			PlayerCallsignData newData = new PlayerCallsignData(player.getUUID().toString(), player.getGameProfile().getName(), generatedCallsign, licenseClass);
			playerSavedData.setCallsignData(player.getUUID(), newData);
			sendGeneratedCallsignMessage(source, player.getGameProfile().getName(), generatedCallsign, licenseClass);
			generatedCount++;
		}

		if (generatedCount == 0) {
			source.sendSuccess(() -> Component.translatable(Radiocraft.translationKey("commands", "callsign.generate.online.none")), true);
		}

		return Math.max(generatedCount, 1);
	}

	private static void sendGeneratedCallsignMessage(CommandSourceStack source, String playerName, String callsign, LicenseClass licenseClass) {
		Component nameComponent = Component.literal(playerName).withStyle(ChatFormatting.WHITE);
		Component callsignComponent = Component.literal(callsign).withStyle(ChatFormatting.GREEN);
		Component licenseComponent = Component.translatable(Radiocraft.translationKey("license_class", licenseClass.name())).withStyle(ChatFormatting.GREEN);
		Supplier<Component> combined = () -> Component.translatable(Radiocraft.translationKey("commands", "callsign.generate.success"), nameComponent, callsignComponent, licenseComponent);
		source.sendSuccess(combined, true);
	}

	private static void sendExistingCallsignMessage(CommandSourceStack source, String playerName, PlayerCallsignData existingData) {
		Component nameComponent = Component.literal(playerName).withStyle(ChatFormatting.WHITE);
		Component callsignComponent = Component.literal(existingData.callsign()).withStyle(ChatFormatting.GRAY);
		Component licenseComponent = Component.translatable(Radiocraft.translationKey("license_class", existingData.licenseClass().name())).withStyle(ChatFormatting.GRAY);
		Supplier<Component> combined = () -> Component.translatable(Radiocraft.translationKey("commands", "callsign.generate.existing"), nameComponent, callsignComponent, licenseComponent);
		source.sendSuccess(combined, true);
	}

	private static boolean handleCallsignCollision(CommandSourceStack source, UUID targetUuid, String targetName, String callsign) {
		PlayerCallsignData collisionPlayer = playerSavedData.getCallsignData(callsign);
		if (collisionPlayer != null && !collisionPlayer.playerUUID().equals(targetUuid.toString())) {
			String existingName = Objects.requireNonNullElse(collisionPlayer.playerName(), collisionPlayer.playerUUID());
			Component existingOwner = Component.literal(existingName).withStyle(ChatFormatting.WHITE);
			sendCollisionMessage(source, targetName, callsign, existingOwner);
			return true;
		}

		BlockEntityCallsignData collisionBlock = blockEntitySavedData.getCallsignData(callsign);
		if (collisionBlock != null) {
			String location = collisionBlock.pos().dimension().location() + " " + collisionBlock.pos().pos().toShortString();
			Component existingOwner = Component.literal(location).withStyle(ChatFormatting.GRAY);
			sendCollisionMessage(source, targetName, callsign, existingOwner);
			return true;
		}

		return false;
	}

	private static void sendCollisionMessage(CommandSourceStack source, String playerName, String callsign, Component existingOwner) {
		Component nameComponent = Component.literal(playerName).withStyle(ChatFormatting.WHITE);
		Component callsignComponent = Component.literal(callsign).withStyle(ChatFormatting.GRAY);
		Component manualComponent = Component.literal("/callsign set " + playerName + " <callsign> <license>").withStyle(ChatFormatting.YELLOW);
		Component message = Component.translatable(Radiocraft.translationKey("commands", "callsign.generate.collision"), nameComponent, callsignComponent, existingOwner, manualComponent);
		source.sendFailure(message);
	}

	private static String generateCallsignFromUuid(UUID playerUUID) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(playerUUID.toString().getBytes(StandardCharsets.UTF_8));
			int digit = Byte.toUnsignedInt(hash[0]) % 10;
			char firstLetter = (char) ('A' + (Byte.toUnsignedInt(hash[1]) % 26));
			char secondLetter = (char) ('A' + (Byte.toUnsignedInt(hash[2]) % 26));
			char thirdLetter = (char) ('A' + (Byte.toUnsignedInt(hash[3]) % 26));
			return "TS" + digit + firstLetter + secondLetter + thirdLetter;
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Unable to generate deterministic callsign", exception);
		}
	}

	public static GameProfile getTarget(CommandSourceStack source, Collection<GameProfile> gameProfiles) throws CommandSyntaxException {
		return gameProfiles == null || gameProfiles.isEmpty() ?
				source.getPlayerOrException().getGameProfile() :
				gameProfiles.stream().findFirst().get();
	}

    private static boolean isPrivileged(@NotNull ServerPlayer player) {
        if (player.serverLevel().getServer().isSingleplayer()) {
            return true;
        }
        // https://minecraft.fandom.com/wiki/Permission_level
        return player.hasPermissions(3);
    }
}
